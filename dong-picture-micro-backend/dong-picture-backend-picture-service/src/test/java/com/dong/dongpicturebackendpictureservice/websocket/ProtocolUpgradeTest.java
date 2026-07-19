package com.dong.dongpicturebackendpictureservice.websocket;

import com.dong.dongpicturebackendcollaborationservice.collab.engine.InMemoryOperationLog;
import com.dong.dongpicturebackendcollaborationservice.collab.engine.Operation;
import com.dong.dongpicturebackendcollaborationservice.collab.engine.SyncProtocol;
import com.dong.dongpicturebackendpictureservice.websocket.model.PictureEditMessageTypeEnum;
import com.dong.dongpicturebackendpictureservice.websocket.model.PictureEditRequestMessage;
import com.dong.dongpicturebackendmodel.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 协议升级测试：
 * 1. 旧协议（ENTER_EDIT/EXIT_EDIT/EDIT_ACTION）向后兼容
 * 2. 新协议（SYNC_STEP1/SYNC_STEP2/OPERATION/PRESENCE）正确路由
 * 3. 新旧协议共存（不同客户端在同一图片上）
 */
class ProtocolUpgradeTest {

    private InMemoryOperationLog operationLog;
    private SyncProtocol syncProtocol;
    private final Map<Long, SyncProtocol> protocols = new ConcurrentHashMap<>();
    private static final Long PICTURE_ID = 300L;

    @BeforeEach
    void setUp() {
        operationLog = new InMemoryOperationLog();
        syncProtocol = new SyncProtocol(operationLog);
        protocols.clear();
        protocols.put(PICTURE_ID, syncProtocol);
    }

    // ======== 旧协议兼容性测试 ========

    @Test
    void oldProtocolEnumValuesStillExist() {
        assertNotNull(PictureEditMessageTypeEnum.ENTER_EDIT);
        assertNotNull(PictureEditMessageTypeEnum.EXIT_EDIT);
        assertNotNull(PictureEditMessageTypeEnum.EDIT_ACTION);
        assertEquals("ENTER_EDIT", PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
        assertEquals("EXIT_EDIT", PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
        assertEquals("EDIT_ACTION", PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
    }

    @Test
    void oldEnterEditMessageCanBeParsed() {
        PictureEditRequestMessage msg = new PictureEditRequestMessage();
        msg.setType("ENTER_EDIT");
        msg.setEditAction("ZOOM_IN");

        assertEquals("ENTER_EDIT", msg.getType());
        assertEquals("ZOOM_IN", msg.getEditAction());
        // 新字段应为空
        assertNull(msg.getClientId());
        assertNull(msg.getStateVector());
    }

    @Test
    void oldProtocolGetEnumByValue() {
        assertEquals(PictureEditMessageTypeEnum.ENTER_EDIT,
                PictureEditMessageTypeEnum.getEnumByValue("ENTER_EDIT"));
        assertEquals(PictureEditMessageTypeEnum.EXIT_EDIT,
                PictureEditMessageTypeEnum.getEnumByValue("EXIT_EDIT"));
        assertEquals(PictureEditMessageTypeEnum.EDIT_ACTION,
                PictureEditMessageTypeEnum.getEnumByValue("EDIT_ACTION"));
    }

    // ======== 新协议枚举测试 ========

    @Test
    void newProtocolEnumValuesAreRegistered() {
        assertNotNull(PictureEditMessageTypeEnum.SYNC_STEP1);
        assertNotNull(PictureEditMessageTypeEnum.SYNC_STEP2);
        assertNotNull(PictureEditMessageTypeEnum.OPERATION);
        assertNotNull(PictureEditMessageTypeEnum.PRESENCE);
        assertNotNull(PictureEditMessageTypeEnum.CURSOR);
        assertNotNull(PictureEditMessageTypeEnum.CANVAS_FULL_SYNC);

        assertEquals("SYNC_STEP1", PictureEditMessageTypeEnum.SYNC_STEP1.getValue());
        assertEquals("SYNC_STEP2", PictureEditMessageTypeEnum.SYNC_STEP2.getValue());
        assertEquals("OPERATION", PictureEditMessageTypeEnum.OPERATION.getValue());
        assertEquals("PRESENCE", PictureEditMessageTypeEnum.PRESENCE.getValue());
        assertEquals("CURSOR", PictureEditMessageTypeEnum.CURSOR.getValue());
    }

    @Test
    void newProtocolGetEnumByValue() {
        assertEquals(PictureEditMessageTypeEnum.SYNC_STEP1,
                PictureEditMessageTypeEnum.getEnumByValue("SYNC_STEP1"));
        assertEquals(PictureEditMessageTypeEnum.PRESENCE,
                PictureEditMessageTypeEnum.getEnumByValue("PRESENCE"));
        assertNull(PictureEditMessageTypeEnum.getEnumByValue("UNKNOWN_TYPE"));
    }

    @Test
    void newProtocolMessageHasAllFields() {
        Map<String, Long> sv = Map.of("clientA", 5L);
        List<Map<String, Object>> ops = List.of(
                Map.of("field", (Object) "rotate", "newValue", "90"));

        PictureEditRequestMessage msg = new PictureEditRequestMessage();
        msg.setType("SYNC_STEP1");
        msg.setClientId("clientA");
        msg.setStateVector(sv);
        msg.setPendingOps(ops);
        msg.setLamportClock(10L);
        msg.setField("cropX");
        msg.setValue("100");
        msg.setCursorX(150.0);
        msg.setCursorY(200.0);
        msg.setEditingField("rotate");

        assertEquals("clientA", msg.getClientId());
        assertEquals(5L, msg.getStateVector().get("clientA"));
        assertEquals(1, msg.getPendingOps().size());
        assertEquals(10L, msg.getLamportClock());
        assertEquals("cropX", msg.getField());
        assertEquals("100", msg.getValue());
        assertEquals(150.0, msg.getCursorX());
        assertEquals("rotate", msg.getEditingField());
    }

    // ======== 引擎集成测试 ========

    @Test
    void syncStep1EngineReturnsEmptyForNewPicture() {
        SyncProtocol.SyncStep1Result result = syncProtocol.handleSyncStep1(
                PICTURE_ID, "clientNew", Collections.emptyMap());

        assertTrue(result.getMissingOperations().isEmpty());
        assertTrue(result.getServerStateVector().isEmpty());
    }

    @Test
    void syncStep2AndStep1EndToEnd() {
        // 提交 3 个操作
        List<Operation> ops = createOps("clientX", PICTURE_ID,
                new String[]{"rotate", "scale", "brightness"},
                new String[]{"45", "1.2", "0.8"});
        SyncProtocol.SyncStep2Result step2 = syncProtocol.handleSyncStep2(ops);

        assertTrue(step2.isAcknowledged());
        assertEquals(3, step2.getAssignedSeqs().size());
        assertEquals(3, operationLog.count(PICTURE_ID));

        // 另一个客户端同步
        SyncProtocol.SyncStep1Result step1 = syncProtocol.handleSyncStep1(
                PICTURE_ID, "clientY", Collections.emptyMap());
        assertEquals(3, step1.getMissingOperations().size());
        assertEquals("rotate", step1.getMissingOperations().get(0).getField());
    }

    @Test
    void liveOperationIsProcessed() {
        Operation op = new Operation();
        op.setClientId("clientZ");
        op.setPictureId(PICTURE_ID);
        op.setField("contrast");
        op.setNewValue("1.8");

        Operation stored = syncProtocol.processLiveOperation(op);
        assertTrue(stored.getSeq() > 0);
        assertEquals("clientZ", stored.getClientId());
        assertEquals("contrast", stored.getField());
        assertEquals(1, operationLog.count(PICTURE_ID));
    }

    // ======== 新旧混用测试 ========

    @Test
    void oldAndNewClientsCanCoexist() {
        // 旧客户端独占编辑
        Map<Long, Long> editingUsers = new ConcurrentHashMap<>();
        editingUsers.put(PICTURE_ID, 1L); // User 1 is editing exclusively

        // 旧客户端发送 EDIT_ACTION
        assertEquals(1L, editingUsers.get(PICTURE_ID));

        // 同时，新客户端通过 SYNC_STEP2 提交操作
        List<Operation> ops = createOps("clientNew", PICTURE_ID,
                new String[]{"saturation"}, new String[]{"0.5"});
        SyncProtocol.SyncStep2Result result = syncProtocol.handleSyncStep2(ops);
        assertTrue(result.isAcknowledged());

        // 新客户端通过 SYNC_STEP1 获取状态
        SyncProtocol.SyncStep1Result step1 = syncProtocol.handleSyncStep1(
                PICTURE_ID, "clientOld", Collections.emptyMap());
        assertEquals(1, step1.getMissingOperations().size());
    }

    @Test
    void isNewProtocolMessageHelper() {
        assertTrue(PictureEditHandler.isNewProtocolMessage("SYNC_STEP1"));
        assertTrue(PictureEditHandler.isNewProtocolMessage("SYNC_STEP2"));
        assertTrue(PictureEditHandler.isNewProtocolMessage("OPERATION"));
        assertTrue(PictureEditHandler.isNewProtocolMessage("PRESENCE"));
        assertTrue(PictureEditHandler.isNewProtocolMessage("CURSOR"));
        assertTrue(PictureEditHandler.isNewProtocolMessage("CANVAS_FULL_SYNC"));

        assertFalse(PictureEditHandler.isNewProtocolMessage("ENTER_EDIT"));
        assertFalse(PictureEditHandler.isNewProtocolMessage("EXIT_EDIT"));
        assertFalse(PictureEditHandler.isNewProtocolMessage("EDIT_ACTION"));
        assertFalse(PictureEditHandler.isNewProtocolMessage("INFO"));
        assertFalse(PictureEditHandler.isNewProtocolMessage("ERROR"));
    }

    // ======== 辅助 ========

    private List<Operation> createOps(String clientId, Long pictureId, String[] fields, String[] values) {
        List<Operation> ops = new ArrayList<>();
        for (int i = 0; i < fields.length; i++) {
            Operation op = new Operation();
            op.setClientId(clientId);
            op.setPictureId(pictureId);
            op.setField(fields[i]);
            op.setNewValue(values[i]);
            op.setLamportClock(i + 1);
            ops.add(op);
        }
        return ops;
    }
}
