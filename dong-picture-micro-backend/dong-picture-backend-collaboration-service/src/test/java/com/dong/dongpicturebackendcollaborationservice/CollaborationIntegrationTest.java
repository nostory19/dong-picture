package com.dong.dongpicturebackendcollaborationservice;

import com.dong.dongpicturebackendcollaborationservice.collab.engine.*;
import com.dong.dongpicturebackendcollaborationservice.collab.sync.CollaborationSession;
import com.dong.dongpicturebackendcollaborationservice.collab.sync.PresenceTracker;
import com.dong.dongpicturebackendcollaborationservice.collab.sync.SyncStateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试：验证协作引擎各组件的端到端流程。
 *
 * 模拟场景：
 * 1. ClientA 和 ClientB 同时编辑同一张图片
 * 2. 通过 SyncProtocol 三阶段同步
 * 3. 通过 PresenceTracker 追踪在线状态
 * 4. 通过 CollaborationSession 管理房间
 */
class CollaborationIntegrationTest {

    private InMemoryOperationLog operationLog;
    private CollaborationSession sessionManager;
    private PresenceTracker presenceTracker;
    private SyncStateManager syncStateManager;
    private SyncProtocol syncProtocol;

    private static final Long PICTURE_ID = 200L;

    @BeforeEach
    void setUp() {
        operationLog = new InMemoryOperationLog();
        sessionManager = new CollaborationSession();
        presenceTracker = new PresenceTracker();
        syncStateManager = new SyncStateManager(operationLog);
        syncProtocol = syncStateManager.getOrCreateSyncProtocol(PICTURE_ID);
    }

    @Test
    void fullCollaborationFlow() {
        // === Phase 1: ClientA 加入 ===
        presenceTracker.join(PICTURE_ID, "clientA",
                new CRDTDocument.ClientPresence("clientA", 1L, "UserA"));
        assertEquals(1, presenceTracker.getOnlineCount(PICTURE_ID));

        // === Phase 2: ClientA 首次同步 ===
        Map<String, Long> emptySV = Collections.emptyMap();
        SyncProtocol.SyncStep1Result step1A = syncProtocol.handleSyncStep1(
                PICTURE_ID, "clientA", emptySV);
        assertTrue(step1A.getMissingOperations().isEmpty());

        // === Phase 3: ClientA 离线编辑后同步 ===
        List<Operation> aOfflineOps = createOps("clientA", PICTURE_ID,
                new String[]{"rotate", "scale", "brightness"},
                new String[]{"45", "1.2", "0.8"});
        SyncProtocol.SyncStep2Result step2A = syncProtocol.handleSyncStep2(aOfflineOps);
        assertTrue(step2A.isAcknowledged());
        assertEquals(3, step2A.getAssignedSeqs().size());
        assertEquals(3, operationLog.count(PICTURE_ID));

        // === Phase 4: ClientB 加入并同步 ===
        presenceTracker.join(PICTURE_ID, "clientB",
                new CRDTDocument.ClientPresence("clientB", 2L, "UserB"));
        assertEquals(2, presenceTracker.getOnlineCount(PICTURE_ID));

        SyncProtocol.SyncStep1Result step1B = syncProtocol.handleSyncStep1(
                PICTURE_ID, "clientB", emptySV);
        // ClientB 应收到 ClientA 的 3 个操作
        assertEquals(3, step1B.getMissingOperations().size());
        assertEquals("rotate", step1B.getMissingOperations().get(0).getField());
        assertEquals("scale", step1B.getMissingOperations().get(1).getField());
        assertEquals("brightness", step1B.getMissingOperations().get(2).getField());

        // === Phase 5: 实时协作 — 双方同时操作 ===
        Operation liveOpA = createOp("clientA", PICTURE_ID, "contrast", "1.5");
        Operation storedA = syncProtocol.processLiveOperation(liveOpA);
        assertTrue(storedA.getSeq() > 0);

        Operation liveOpB = createOp("clientB", PICTURE_ID, "saturation", "0.7");
        Operation storedB = syncProtocol.processLiveOperation(liveOpB);
        assertTrue(storedB.getSeq() > 0);

        // === Phase 6: 增量同步验证 ===
        Map<String, Long> partialSV = Map.of("clientA", 3L); // ClientA 只有前 3 个操作
        SyncProtocol.SyncStep1Result partialSync = syncProtocol.handleSyncStep1(
                PICTURE_ID, "clientA", partialSV);
        // 缺失 seq 4,5（来自 A 和 B 的实时操作）
        assertEquals(2, partialSync.getMissingOperations().size());

        // === Phase 7: 状态向量一致性 ===
        Map<String, Long> globalSV = syncProtocol.getGlobalStateVector();
        assertTrue(globalSV.size() >= 2); // clientA 和 clientB 都被追踪
        assertTrue(globalSV.get("clientA") >= 1);
        assertTrue(globalSV.get("clientB") >= 1);

        // === Phase 8: ClientA 离开 ===
        presenceTracker.leave(PICTURE_ID, "clientA");
        assertEquals(1, presenceTracker.getOnlineCount(PICTURE_ID));
        assertEquals("UserB", presenceTracker.getOnlineClients(PICTURE_ID).get(0).getUserName());

        // === Phase 9: 操作总数验证 ===
        assertEquals(5, operationLog.count(PICTURE_ID)); // 3 offline + 2 live
    }

    @Test
    void presenceTrackingAcrossMultiplePictures() {
        Long pic1 = 1L, pic2 = 2L;

        presenceTracker.join(pic1, "clientA",
                new CRDTDocument.ClientPresence("clientA", 1L, "A"));
        presenceTracker.join(pic2, "clientB",
                new CRDTDocument.ClientPresence("clientB", 2L, "B"));

        assertEquals(1, presenceTracker.getOnlineCount(pic1));
        assertEquals(1, presenceTracker.getOnlineCount(pic2));

        presenceTracker.updatePresence(pic1, "clientA", "rotate", 100, 200, null);
        CRDTDocument.ClientPresence p = presenceTracker.getPresence(pic1, "clientA");
        assertEquals("rotate", p.getEditingField());
        assertEquals(100, p.getCursorX());
        assertEquals(200, p.getCursorY());

        // 跨图片不影响
        assertNull(presenceTracker.getPresence(pic2, "clientA"));
    }

    @Test
    void heartbeatEviction() throws Exception {
        presenceTracker.join(PICTURE_ID, "clientX",
                new CRDTDocument.ClientPresence("clientX", 1L, "X"));
        assertEquals(1, presenceTracker.getOnlineCount(PICTURE_ID));

        // 等待至少 1ms，确保心跳时间戳已过期
        Thread.sleep(2);

        List<Map.Entry<Long, String>> evicted = presenceTracker.evictStaleSessions(1);
        assertEquals(1, evicted.size());
        assertEquals(PICTURE_ID, evicted.get(0).getKey());
        assertEquals("clientX", evicted.get(0).getValue());
        assertEquals(0, presenceTracker.getOnlineCount(PICTURE_ID));
    }

    @Test
    void syncStateManagerCreatesDistinctProtocolsPerPicture() {
        SyncProtocol p1 = syncStateManager.getOrCreateSyncProtocol(1L);
        SyncProtocol p2 = syncStateManager.getOrCreateSyncProtocol(2L);
        assertNotSame(p1, p2);

        CRDTDocument d1 = syncStateManager.getOrCreateDocument(1L);
        assertEquals(1L, d1.getPictureId());

        // documents 计数：只有 getOrCreateDocument(1L) 创建了 1 个文档
        assertEquals(1, syncStateManager.getActiveDocumentCount());
    }

    // ======== 辅助方法 ========

    private List<Operation> createOps(String clientId, Long pictureId,
                                       String[] fields, String[] values) {
        List<Operation> ops = new ArrayList<>();
        for (int i = 0; i < fields.length; i++) {
            ops.add(createOp(clientId, pictureId, fields[i], values[i]));
        }
        return ops;
    }

    private Operation createOp(String clientId, Long pictureId, String field, String newValue) {
        Operation op = new Operation();
        op.setClientId(clientId);
        op.setPictureId(pictureId);
        op.setField(field);
        op.setNewValue(newValue);
        op.setOldValue("");
        op.setLamportClock(System.currentTimeMillis() % 10000);
        return op;
    }
}
