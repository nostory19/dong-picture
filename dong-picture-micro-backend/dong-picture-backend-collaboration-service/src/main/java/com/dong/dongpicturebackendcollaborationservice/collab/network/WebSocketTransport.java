package com.dong.dongpicturebackendcollaborationservice.collab.network;

import cn.hutool.json.JSONUtil;
import com.dong.dongpicturebackendcollaborationservice.collab.engine.CRDTDocument;
import com.dong.dongpicturebackendcollaborationservice.collab.engine.Operation;
import com.dong.dongpicturebackendcollaborationservice.collab.engine.OperationLog;
import com.dong.dongpicturebackendcollaborationservice.collab.engine.SyncProtocol;
import com.dong.dongpicturebackendcollaborationservice.collab.sync.CollaborationSession;
import com.dong.dongpicturebackendcollaborationservice.collab.sync.PresenceTracker;
import com.dong.dongpicturebackendcollaborationservice.collab.sync.SyncStateManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协作 WebSocket 传输层。
 *
 * 替换原有的 PictureEditHandler，使用新的 CRDT 协作引擎：
 * - 三阶段同步协议（SYNC_STEP1 / SYNC_STEP2）
 * - 实时操作广播（OPERATION）
 * - 在线 Presence 追踪（PRESENCE / HEARTBEAT）
 * - 心跳保活
 */
@Slf4j
public class WebSocketTransport extends TextWebSocketHandler {

    private final CollaborationSession sessionManager;
    private final PresenceTracker presenceTracker;
    private final SyncStateManager syncStateManager;
    private final OperationLog operationLog;

    /** WebSocket session → clientId */
    private final ConcurrentHashMap<String, String> sessionClientMap = new ConcurrentHashMap<>();

    /** WebSocket session → pictureId */
    private final ConcurrentHashMap<String, Long> sessionPictureMap = new ConcurrentHashMap<>();

    public WebSocketTransport(CollaborationSession sessionManager,
                              PresenceTracker presenceTracker,
                              SyncStateManager syncStateManager,
                              OperationLog operationLog) {
        this.sessionManager = sessionManager;
        this.presenceTracker = presenceTracker;
        this.syncStateManager = syncStateManager;
        this.operationLog = operationLog;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        Long userId = (Long) session.getAttributes().get("userId");
        if (pictureId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        // 从参数中获取 clientId（客户端持久化的 UUID）
        String clientId = getClientIdFromSession(session);
        String userName = getUserNameFromSession(session);

        // 注册到房间
        sessionManager.join(pictureId, session);
        sessionClientMap.put(session.getId(), clientId);
        sessionPictureMap.put(session.getId(), pictureId);

        // 注册 Presence
        CRDTDocument.ClientPresence presence = new CRDTDocument.ClientPresence(
                clientId, userId, userName != null ? userName : "用户" + userId);
        presenceTracker.join(pictureId, clientId, presence);

        log.info("Client {} joined picture {}, total online: {}",
                clientId, pictureId, sessionManager.getSessionCount(pictureId));

        // 通知其他用户：有人加入
        CollabMessage joinMsg = buildCollabMessage(CollabMessageType.CLIENT_JOIN, pictureId);
        joinMsg.setClientId(clientId);
        joinMsg.setMessage("用户 " + (userName != null ? userName : clientId) + " 加入了协作");
        broadcastToPicture(pictureId, joinMsg, session);

        // 向新加入的客户端发送当前在线用户列表
        CollabMessage presenceMsg = buildCollabMessage(CollabMessageType.INFO, pictureId);
        presenceMsg.setMessage("已连接协作画布，在线人数: " + presenceTracker.getOnlineCount(pictureId));
        List<Map<String, Object>> onlineClients = new ArrayList<>();
        for (CRDTDocument.ClientPresence p : presenceTracker.getOnlineClients(pictureId)) {
            Map<String, Object> client = new HashMap<>();
            client.put("clientId", p.getClientId());
            client.put("userName", p.getUserName());
            client.put("userAvatar", p.getUserAvatar());
            client.put("editingField", p.getEditingField());
            onlineClients.add(client);
        }
        presenceMsg.setOnlineClients(onlineClients);
        sendMessage(session, presenceMsg);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String clientId = sessionClientMap.remove(session.getId());
        Long pictureId = sessionPictureMap.remove(session.getId());
        if (pictureId == null) return;

        sessionManager.leave(pictureId, session);
        if (clientId != null) {
            presenceTracker.leave(pictureId, clientId);
        }

        log.info("Client {} left picture {}, remaining: {}",
                clientId, pictureId, sessionManager.getSessionCount(pictureId));

        // 通知其他人：有人离开
        CollabMessage leaveMsg = buildCollabMessage(CollabMessageType.CLIENT_LEAVE, pictureId);
        leaveMsg.setClientId(clientId);
        leaveMsg.setMessage("用户 " + (clientId != null ? clientId : "未知") + " 离开了协作");
        broadcastToPicture(pictureId, leaveMsg, null);

        // 清理空闲文档
        syncStateManager.evictIfIdle(pictureId, sessionManager);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        CollabMessage msg;
        try {
            msg = JSONUtil.toBean(textMessage.getPayload(), CollabMessage.class);
        } catch (Exception e) {
            sendError(session, "消息格式错误");
            return;
        }

        String clientId = msg.getClientId() != null ? msg.getClientId()
                : sessionClientMap.get(session.getId());
        Long pictureId = msg.getPictureId() != null ? msg.getPictureId()
                : sessionPictureMap.get(session.getId());

        if (pictureId == null) {
            sendError(session, "未指定图片 ID");
            return;
        }

        // 更新 session→client 映射
        if (msg.getClientId() != null) {
            sessionClientMap.put(session.getId(), msg.getClientId());
        }

        String type = msg.getType();
        if (type == null) {
            sendError(session, "消息类型不能为空");
            return;
        }

        switch (type) {
            case CollabMessageType.SYNC_STEP1:
                handleSyncStep1(session, msg, pictureId, clientId);
                break;
            case CollabMessageType.SYNC_STEP2:
                handleSyncStep2(session, msg, pictureId, clientId);
                break;
            case CollabMessageType.OPERATION:
                handleOperation(session, msg, pictureId, clientId);
                break;
            case CollabMessageType.PRESENCE:
                handlePresence(session, msg, pictureId, clientId);
                break;
            case CollabMessageType.HEARTBEAT:
                handleHeartbeat(session, pictureId, clientId);
                break;
            default:
                sendError(session, "未知消息类型: " + type);
        }
    }

    // ======== 三阶段同步 ========

    private void handleSyncStep1(WebSocketSession session, CollabMessage msg,
                                  Long pictureId, String clientId) throws IOException {
        SyncProtocol protocol = syncStateManager.getOrCreateSyncProtocol(pictureId);
        Map<String, Long> clientSV = msg.getStateVector() != null ? msg.getStateVector()
                : Collections.emptyMap();

        SyncProtocol.SyncStep1Result result = protocol.handleSyncStep1(pictureId, clientId, clientSV);

        CollabMessage response = buildCollabMessage(CollabMessageType.SYNC_STEP1, pictureId);
        response.setServerStateVector(result.getServerStateVector());
        response.setMissingOps(opsToMaps(result.getMissingOperations()));
        response.setMessage("同步完成，收到 " + result.getMissingOperations().size() + " 条操作");
        sendMessage(session, response);
    }

    private void handleSyncStep2(WebSocketSession session, CollabMessage msg,
                                  Long pictureId, String clientId) throws IOException {
        SyncProtocol protocol = syncStateManager.getOrCreateSyncProtocol(pictureId);

        List<Operation> ops = mapsToOps(msg.getPendingOps(), clientId, pictureId);
        SyncProtocol.SyncStep2Result result = protocol.handleSyncStep2(ops);

        CollabMessage response = buildCollabMessage(CollabMessageType.SYNC_STEP2, pictureId);
        response.setAcknowledged(result.isAcknowledged());
        response.setServerStateVector(result.getServerStateVector());
        response.setAssignedSeqs(result.getAssignedSeqs());
        sendMessage(session, response);

        // 广播这些新操作给房间内其他客户端
        for (Operation op : ops) {
            CollabMessage broadcast = buildCollabMessage(CollabMessageType.OPERATION, pictureId);
            broadcast.setClientId(op.getClientId());
            broadcast.setField(op.getField());
            broadcast.setNewValue(op.getNewValue());
            broadcast.setOldValue(op.getOldValue());
            broadcast.setLamportClock(op.getSeq());
            broadcastToPicture(pictureId, broadcast, session);
        }
    }

    // ======== 实时操作 ========

    private void handleOperation(WebSocketSession session, CollabMessage msg,
                                  Long pictureId, String clientId) throws IOException {
        SyncProtocol protocol = syncStateManager.getOrCreateSyncProtocol(pictureId);

        Operation op = new Operation();
        op.setClientId(clientId);
        op.setLamportClock(msg.getLamportClock() != null ? msg.getLamportClock() : 0);
        op.setPictureId(pictureId);
        op.setField(msg.getField());
        op.setNewValue(msg.getNewValue());
        op.setOldValue(msg.getOldValue());

        Operation stored = protocol.processLiveOperation(op);

        // 广播给其他客户端
        CollabMessage broadcast = buildCollabMessage(CollabMessageType.OPERATION, pictureId);
        broadcast.setClientId(stored.getClientId());
        broadcast.setField(stored.getField());
        broadcast.setNewValue(stored.getNewValue());
        broadcast.setOldValue(stored.getOldValue());
        broadcast.setLamportClock(stored.getSeq());
        broadcastToPicture(pictureId, broadcast, session);
    }

    // ======== Presence & Heartbeat ========

    private void handlePresence(WebSocketSession session, CollabMessage msg,
                                 Long pictureId, String clientId) {
        presenceTracker.updatePresence(pictureId, clientId,
                msg.getEditingField(),
                msg.getCursorX() != null ? msg.getCursorX() : 0,
                msg.getCursorY() != null ? msg.getCursorY() : 0,
                msg.getSelection());

        // 广播 Presence 变更给其他客户端
        CollabMessage broadcast = buildCollabMessage(CollabMessageType.PRESENCE, pictureId);
        broadcast.setClientId(clientId);
        broadcast.setEditingField(msg.getEditingField());
        broadcast.setCursorX(msg.getCursorX());
        broadcast.setCursorY(msg.getCursorY());
        broadcast.setSelection(msg.getSelection());
        broadcastToPicture(pictureId, broadcast, session);
    }

    private void handleHeartbeat(WebSocketSession session, Long pictureId, String clientId) {
        presenceTracker.updatePresence(pictureId, clientId, null, 0, 0, null);
    }

    // ======== 广播工具 ========

    private void broadcastToPicture(Long pictureId, CollabMessage message, WebSocketSession excludeSession) {
        String json = JSONUtil.toJsonStr(message);
        TextMessage textMessage = new TextMessage(json);
        for (WebSocketSession s : sessionManager.getSessions(pictureId)) {
            if (s.isOpen() && (excludeSession == null || !s.getId().equals(excludeSession.getId()))) {
                try {
                    synchronized (s) {
                        s.sendMessage(textMessage);
                    }
                } catch (IOException e) {
                    log.error("Failed to send message to session {}", s.getId(), e);
                }
            }
        }
    }

    private void sendMessage(WebSocketSession session, CollabMessage message) throws IOException {
        String json = JSONUtil.toJsonStr(message);
        synchronized (session) {
            session.sendMessage(new TextMessage(json));
        }
    }

    private void sendError(WebSocketSession session, String errMsg) throws IOException {
        CollabMessage msg = new CollabMessage();
        msg.setType(CollabMessageType.ERROR);
        msg.setMessage(errMsg);
        sendMessage(session, msg);
    }

    // ======== 辅助方法 ========

    private CollabMessage buildCollabMessage(String type, Long pictureId) {
        CollabMessage msg = new CollabMessage();
        msg.setType(type);
        msg.setPictureId(pictureId);
        return msg;
    }

    private String getClientIdFromSession(WebSocketSession session) {
        // 优先从查询参数获取，其次从属性获取
        String clientId = (String) session.getAttributes().get("clientId");
        if (clientId == null && session.getUri() != null) {
            String query = session.getUri().getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=", 2);
                    if (kv.length == 2 && "clientId".equals(kv[0])) {
                        clientId = kv[1];
                        break;
                    }
                }
            }
        }
        return clientId != null ? clientId : session.getId();
    }

    private String getUserNameFromSession(WebSocketSession session) {
        Object userName = session.getAttributes().get("userName");
        return userName != null ? userName.toString() : null;
    }

    private static List<Map<String, Object>> opsToMaps(List<Operation> ops) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Operation op : ops) {
            Map<String, Object> map = new HashMap<>();
            map.put("seq", op.getSeq());
            map.put("clientId", op.getClientId());
            map.put("field", op.getField());
            map.put("oldValue", op.getOldValue());
            map.put("newValue", op.getNewValue());
            map.put("lamportClock", op.getLamportClock());
            map.put("timestamp", op.getTimestamp());
            result.add(map);
        }
        return result;
    }

    private static List<Operation> mapsToOps(List<Map<String, Object>> maps, String clientId, Long pictureId) {
        if (maps == null) return Collections.emptyList();
        List<Operation> ops = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            Operation op = new Operation();
            op.setClientId(clientId);
            op.setPictureId(pictureId);
            op.setField((String) map.get("field"));
            op.setOldValue((String) map.get("oldValue"));
            op.setNewValue((String) map.get("newValue"));
            Object lc = map.get("lamportClock");
            op.setLamportClock(lc != null ? ((Number) lc).longValue() : 0);
            ops.add(op);
        }
        return ops;
    }
}
