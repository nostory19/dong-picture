package com.dong.dongpicturebackendcollaborationservice;

import com.dong.dongpicturebackendcollaborationservice.collab.engine.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 核心测试：模拟两个客户端并发 1000 次操作，验证通过 SyncProtocol
 * 同步后两个客户端的状态（操作序列）完全一致。
 *
 * 测试场景：
 *   1. ClientA 和 ClientB 各自本地产生 500 个操作（共 1000 并发操作）
 *   2. 双方通过 SyncProtocol 同步到服务端
 *   3. 双方再从服务端拉取所有操作
 *   4. 验证两边的操作序列完全一致（数量相同、seq 连续不丢、内容相同）
 */
class ConcurrentConvergenceTest {

    private InMemoryOperationLog operationLog;
    private SyncProtocol syncProtocol;
    private static final Long PICTURE_ID = 1L;

    @BeforeEach
    void setUp() {
        operationLog = new InMemoryOperationLog();
        syncProtocol = new SyncProtocol(operationLog);
    }

    @Test
    void twoClients1000ConcurrentOpsShouldConverge() throws Exception {
        int opsPerClient = 500;
        int numClients = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numClients);

        // 每个客户端本地的操作列表（从服务端同步前）
        ConcurrentHashMap<String, List<Operation>> localOps = new ConcurrentHashMap<>();

        CountDownLatch submitLatch = new CountDownLatch(numClients);
        CountDownLatch queryLatch = new CountDownLatch(numClients);
        CyclicBarrier submitBarrier = new CyclicBarrier(numClients);
        CyclicBarrier queryBarrier = new CyclicBarrier(numClients);

        // 模拟 ClientA 和 ClientB 同时发起同步
        for (int c = 0; c < numClients; c++) {
            final String clientId = "client" + (char) ('A' + c);
            executor.submit(() -> {
                try {
                    List<Operation> myOps = new ArrayList<>();
                    for (int i = 0; i < opsPerClient; i++) {
                        myOps.add(Operation.builder()
                                .clientId(clientId)
                                .lamportClock(i + 1)
                                .pictureId(PICTURE_ID)
                                .field(randomField())
                                .oldValue(String.valueOf(i))
                                .newValue(String.valueOf(i + 1))
                                .build());
                    }

                    // 阶段1: 等所有线程准备好后同时提交
                    submitBarrier.await();
                    SyncProtocol.SyncStep2Result step2 = syncProtocol.handleSyncStep2(myOps);
                    assertTrue(step2.isAcknowledged());
                    submitLatch.countDown();

                    // 等所有线程都提交完毕
                    submitLatch.await(30, TimeUnit.SECONDS);

                    // 阶段2: 同时也拉取全量操作
                    queryBarrier.await();
                    Map<String, Long> emptyState = Collections.emptyMap();
                    SyncProtocol.SyncStep1Result step1 = syncProtocol.handleSyncStep1(
                            PICTURE_ID, clientId, emptyState);

                    localOps.put(clientId, step1.getMissingOperations());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    queryLatch.countDown();
                }
            });
        }

        queryLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // === 验证 ===

        List<Operation> clientAOps = localOps.get("clientA");
        List<Operation> clientBOps = localOps.get("clientB");

        // 1. 双方收到的操作数量应该相同（每个客户端 500 个操作，共 1000 个）
        assertEquals(numClients * opsPerClient, clientAOps.size(),
                "ClientA should have all operations");
        assertEquals(numClients * opsPerClient, clientBOps.size(),
                "ClientB should have all operations");

        // 2. 序号应该连续（1..1000），无空洞
        Set<Long> seqs = clientAOps.stream().map(Operation::getSeq)
                .collect(Collectors.toSet());
        for (long i = 1; i <= numClients * opsPerClient; i++) {
            assertTrue(seqs.contains(i), "Missing sequence number: " + i);
        }
        assertEquals(numClients * opsPerClient, seqs.size());

        // 3. 双方的操作列表应该完全一致（同一顺序）
        for (int i = 0; i < clientAOps.size(); i++) {
            Operation a = clientAOps.get(i);
            Operation b = clientBOps.get(i);
            assertEquals(a.getSeq(), b.getSeq(), "Seq mismatch at index " + i);
            assertEquals(a.getClientId(), b.getClientId(), "ClientId mismatch at index " + i);
            assertEquals(a.getField(), b.getField(), "Field mismatch at index " + i);
            assertEquals(a.getNewValue(), b.getNewValue(), "NewValue mismatch at index " + i);
        }

        // 4. 服务端状态向量应追踪到所有客户端
        Map<String, Long> globalSV = syncProtocol.getGlobalStateVector();
        assertTrue(globalSV.size() >= numClients);

        // 5. 每个客户端的操作数量
        long aCount = clientAOps.stream().filter(op -> "clientA".equals(op.getClientId())).count();
        long bCount = clientAOps.stream().filter(op -> "clientB".equals(op.getClientId())).count();
        assertEquals(opsPerClient, aCount);
        assertEquals(opsPerClient, bCount);

        System.out.println("=== Convergence Test PASSED ===");
        System.out.println("Total ops: " + clientAOps.size());
        System.out.println("ClientA ops: " + aCount + ", ClientB ops: " + bCount);
        System.out.println("Seq range: 1.." + syncProtocol.getServerClock());
    }

    @Test
    void offlineReconnectShouldSyncIncrementally() {
        // 1. ClientB 先提交 10 个操作（模拟 B 在线期间的操作）
        List<Operation> bFirstBatch = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            bFirstBatch.add(Operation.builder()
                    .clientId("clientB").lamportClock(i + 1)
                    .pictureId(PICTURE_ID).field("rotate")
                    .newValue("" + (i * 10)).build());
        }
        syncProtocol.handleSyncStep2(bFirstBatch);

        // 2. ClientA 连接并同步了前 5 个操作 (stateVector: B=5)
        Map<String, Long> aAfterPartial = Map.of("clientB", 5L);
        SyncProtocol.SyncStep1Result partialResult = syncProtocol.handleSyncStep1(
                PICTURE_ID, "clientA", aAfterPartial);
        assertEquals(5, partialResult.getMissingOperations().size());

        // 3. B 继续提交 5 个操作（模拟 A 离线期间 B 的操作）
        List<Operation> bSecondBatch = new ArrayList<>();
        for (int i = 10; i < 15; i++) {
            bSecondBatch.add(Operation.builder()
                    .clientId("clientB").lamportClock(i + 1)
                    .pictureId(PICTURE_ID).field("scale")
                    .newValue("" + (i * 0.1)).build());
        }
        syncProtocol.handleSyncStep2(bSecondBatch);

        // 4. A 重新连接，增量同步（应该只收到 seq 6..15 的操作）
        SyncProtocol.SyncStep1Result reconnectResult = syncProtocol.handleSyncStep1(
                PICTURE_ID, "clientA", aAfterPartial);

        // A 缺：B 的 seq 6..15（通过服务端分配的 seq）
        List<Operation> missing = reconnectResult.getMissingOperations();
        assertEquals(10, missing.size()); // 5 from first batch after seq 5 + 5 from second batch

        // 验证这些操作确实是 B 的且序号递增
        for (Operation op : missing) {
            assertEquals("clientB", op.getClientId());
            assertTrue(op.getSeq() > 5, "All missing ops should have seq > 5");
        }
    }

    @Test
    void interleavedOperationsShouldPreservePerClientOrder() {
        // 模拟 ClientA 和 ClientB 交替提交操作（交错）
        // A1, B1, A2, B2, A3, B3 ...
        for (int i = 0; i < 100; i++) {
            Operation aOp = Operation.builder()
                    .clientId("clientA").lamportClock(i + 1)
                    .pictureId(PICTURE_ID).field("rotate").newValue("" + i).build();
            Operation bOp = Operation.builder()
                    .clientId("clientB").lamportClock(i + 1)
                    .pictureId(PICTURE_ID).field("scale").newValue("" + i).build();
            syncProtocol.handleSyncStep2(List.of(aOp));
            syncProtocol.handleSyncStep2(List.of(bOp));
        }

        // 拉取全量操作
        Map<String, Long> emptyState = Collections.emptyMap();
        SyncProtocol.SyncStep1Result result = syncProtocol.handleSyncStep1(
                PICTURE_ID, "clientC", emptyState);

        List<Operation> allOps = result.getMissingOperations();
        assertEquals(200, allOps.size());

        // 验证每个客户端的操作序号递增
        List<Operation> aOps = allOps.stream()
                .filter(op -> "clientA".equals(op.getClientId())).toList();
        List<Operation> bOps = allOps.stream()
                .filter(op -> "clientB".equals(op.getClientId())).toList();

        for (int i = 1; i < aOps.size(); i++) {
            assertTrue(aOps.get(i).getSeq() > aOps.get(i - 1).getSeq(),
                    "ClientA ops should be in seq order");
        }
        for (int i = 1; i < bOps.size(); i++) {
            assertTrue(bOps.get(i).getSeq() > bOps.get(i - 1).getSeq(),
                    "ClientB ops should be in seq order");
        }
    }

    private static final String[] FIELDS = {
            "rotate", "scale", "cropX", "cropY", "cropW", "cropH",
            "brightness", "contrast", "saturation"
    };

    private String randomField() {
        return FIELDS[(int) (Math.random() * FIELDS.length)];
    }
}
