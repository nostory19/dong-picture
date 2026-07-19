package com.dong.dongpicturebackendcollaborationservice;

import com.dong.dongpicturebackendcollaborationservice.collab.engine.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SyncProtocolTest {

    private InMemoryOperationLog operationLog;
    private SyncProtocol syncProtocol;
    private static final Long PICTURE_ID = 1L;

    @BeforeEach
    void setUp() {
        operationLog = new InMemoryOperationLog();
        syncProtocol = new SyncProtocol(operationLog);
    }

    @Test
    void syncStep1EmptyStateShouldReturnNoMissingOps() {
        // 客户端和服务端都是空的
        Map<String, Long> clientState = Collections.emptyMap();
        SyncProtocol.SyncStep1Result result = syncProtocol.handleSyncStep1(
                PICTURE_ID, "clientA", clientState);

        assertTrue(result.getMissingOperations().isEmpty());
        assertTrue(result.getServerStateVector().isEmpty());
    }

    @Test
    void syncStep2AndStep1ShouldSyncCorrectly() {
        // 客户端 B 离线编辑了 5 个操作，现在上线同步
        List<Operation> bOfflineOps = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Operation op = Operation.builder()
                    .clientId("clientB")
                    .lamportClock(i + 1)
                    .pictureId(PICTURE_ID)
                    .field("rotate")
                    .oldValue(String.valueOf(i * 10))
                    .newValue(String.valueOf((i + 1) * 10))
                    .build();
            bOfflineOps.add(op);
        }

        // Step2: B 把离线操作提交到服务端
        SyncProtocol.SyncStep2Result step2Result = syncProtocol.handleSyncStep2(bOfflineOps);
        assertTrue(step2Result.isAcknowledged());
        assertEquals(5, step2Result.getAssignedSeqs().size());
        assertEquals(5, operationLog.count(PICTURE_ID));

        // Step1: 新客户端 A 连接，stateVector 为空
        Map<String, Long> aEmptyState = Collections.emptyMap();
        SyncProtocol.SyncStep1Result step1Result = syncProtocol.handleSyncStep1(
                PICTURE_ID, "clientA", aEmptyState);

        // A 应该收到所有 5 个操作
        assertEquals(5, step1Result.getMissingOperations().size());
        assertEquals(5L, step1Result.getServerStateVector().get("clientB"));
    }

    @Test
    void incrementalSyncShouldOnlyReturnMissingOps() {
        // 先让 B 提交 5 个操作
        List<Operation> batch1 = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            batch1.add(Operation.builder()
                    .clientId("clientB").lamportClock(i + 1)
                    .pictureId(PICTURE_ID).field("rotate")
                    .newValue("" + i).build());
        }
        syncProtocol.handleSyncStep2(batch1);

        // A 同步了前 3 个
        Map<String, Long> aPartial = Map.of("clientB", 3L);
        SyncProtocol.SyncStep1Result result = syncProtocol.handleSyncStep1(
                PICTURE_ID, "clientA", aPartial);

        // A 应该只收到 seq 4..5 的操作
        assertEquals(2, result.getMissingOperations().size());
        List<Operation> missing = result.getMissingOperations();
        assertEquals(4, missing.get(0).getSeq());
        assertEquals(5, missing.get(1).getSeq());
    }

    @Test
    void processLiveOperationShouldAssignSequenceAndStore() {
        Operation op = Operation.builder()
                .clientId("clientC")
                .lamportClock(1)
                .pictureId(PICTURE_ID)
                .field("scale")
                .newValue("1.5")
                .build();

        Operation stored = syncProtocol.processLiveOperation(op);
        assertTrue(stored.getSeq() > 0);
        assertTrue(stored.getTimestamp() > 0);
        assertEquals(1, operationLog.count(PICTURE_ID));
    }

    @Test
    void globalStateVectorShouldTrackAllClients() {
        // Client A 提交 3 个操作
        syncProtocol.handleSyncStep2(List.of(
                op("clientA", 1), op("clientA", 2), op("clientA", 3)));

        // Client B 提交 2 个操作
        syncProtocol.handleSyncStep2(List.of(
                op("clientB", 1), op("clientB", 2)));

        Map<String, Long> global = syncProtocol.getGlobalStateVector();
        assertEquals(3L, global.get("clientA"));
        assertEquals(5L, global.get("clientB")); // seq 4,5 assigned by server
    }

    private Operation op(String clientId, long lamport) {
        return Operation.builder()
                .clientId(clientId)
                .lamportClock(lamport)
                .pictureId(PICTURE_ID)
                .field("rotate")
                .newValue("" + (lamport * 10))
                .build();
    }
}
