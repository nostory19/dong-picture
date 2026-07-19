package com.dong.dongpicturebackendcollaborationservice.collab.network;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebRTC 信令中继处理器。
 *
 * 服务端不解析 SDP/ICE 内容，只做对端转发：
 *   ClientA → OFFER(target=B) → Server → ClientB
 *   ClientB → ANSWER(target=A) → Server → ClientA
 *   任意方 → ICE_CANDIDATE(target=X) → Server → ClientX
 *
 * 信令端点：/ws/collab/signaling?pictureId=xxx&clientId=xxx
 */
@Slf4j
public class WebRTCSignalingHandler extends TextWebSocketHandler {

    /** pictureId → (clientId → session) */
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, WebSocketSession>> rooms = new ConcurrentHashMap<>();

    /** sessionId → (pictureId, clientId) */
    private final ConcurrentHashMap<String, SessionInfo> sessionInfoMap = new ConcurrentHashMap<>();

    private static class SessionInfo {
        final Long pictureId;
        final String clientId;
        SessionInfo(Long pictureId, String clientId) {
            this.pictureId = pictureId;
            this.clientId = clientId;
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        String clientId = (String) session.getAttributes().get("clientId");
        if (pictureId == null || clientId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        rooms.computeIfAbsent(pictureId, k -> new ConcurrentHashMap<>())
                .put(clientId, session);
        sessionInfoMap.put(session.getId(), new SessionInfo(pictureId, clientId));

        log.info("[RTC Signal] client {} joined room picture={}, room size={}",
                clientId, pictureId,
                rooms.get(pictureId) != null ? rooms.get(pictureId).size() : 0);

        // 通知房间内其他客户端：新 peer 已加入（可发起 P2P 连接）
        SignalMessage joinMsg = new SignalMessage();
        joinMsg.setType("PEER_JOIN");
        joinMsg.setFromClientId(clientId);
        broadcastToRoom(pictureId, joinMsg, clientId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        SignalMessage msg;
        try {
            msg = JSONUtil.toBean(textMessage.getPayload(), SignalMessage.class);
        } catch (Exception e) {
            log.warn("[RTC Signal] bad message: {}", textMessage.getPayload());
            return;
        }

        SessionInfo info = sessionInfoMap.get(session.getId());
        if (info == null) {
            sendError(session, "Not registered");
            return;
        }

        String type = msg.getType();
        if (type == null) return;

        switch (type) {
            case "OFFER":
            case "ANSWER":
            case "ICE_CANDIDATE":
                relayToPeer(info.pictureId, info.clientId, msg);
                break;
            case "PEER_LEAVE":
                broadcastToRoom(info.pictureId, msg, info.clientId);
                break;
            case "PING":
                // 心跳响应
                SignalMessage pong = new SignalMessage();
                pong.setType("PONG");
                sendMessage(session, pong);
                break;
            default:
                log.debug("[RTC Signal] unknown message type: {}", type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        SessionInfo info = sessionInfoMap.remove(session.getId());
        if (info == null) return;

        ConcurrentHashMap<String, WebSocketSession> room = rooms.get(info.pictureId);
        if (room != null) {
            room.remove(info.clientId);
            if (room.isEmpty()) {
                rooms.remove(info.pictureId);
            }
        }

        // 通知房间内其他人
        SignalMessage leaveMsg = new SignalMessage();
        leaveMsg.setType("PEER_LEAVE");
        leaveMsg.setFromClientId(info.clientId);
        broadcastToRoom(info.pictureId, leaveMsg, null);

        log.info("[RTC Signal] client {} left room picture={}", info.clientId, info.pictureId);
    }

    /** 转发消息到指定对端 */
    private void relayToPeer(Long pictureId, String fromClientId, SignalMessage msg) {
        String targetId = msg.getTargetClientId();
        if (targetId == null) return;

        ConcurrentHashMap<String, WebSocketSession> room = rooms.get(pictureId);
        if (room == null) return;

        WebSocketSession targetSession = room.get(targetId);
        if (targetSession == null || !targetSession.isOpen()) {
            log.debug("[RTC Signal] target {} not found in room picture={}", targetId, pictureId);
            return;
        }

        // 标记消息来源
        msg.setFromClientId(fromClientId);

        try {
            sendMessage(targetSession, msg);
        } catch (IOException e) {
            log.error("[RTC Signal] failed to relay to {}: {}", targetId, e.getMessage());
        }
    }

    /** 广播消息到房间内所有客户端（排除 excludeClientId） */
    private void broadcastToRoom(Long pictureId, SignalMessage msg, String excludeClientId) {
        ConcurrentHashMap<String, WebSocketSession> room = rooms.get(pictureId);
        if (room == null) return;

        String json = JSONUtil.toJsonStr(msg);
        for (Map.Entry<String, WebSocketSession> entry : room.entrySet()) {
            if (excludeClientId != null && entry.getKey().equals(excludeClientId)) continue;
            WebSocketSession s = entry.getValue();
            if (s.isOpen()) {
                try {
                    synchronized (s) {
                        s.sendMessage(new TextMessage(json));
                    }
                } catch (IOException e) {
                    log.error("[RTC Signal] broadcast failed: {}", e.getMessage());
                }
            }
        }
    }

    private void sendMessage(WebSocketSession session, SignalMessage msg) throws IOException {
        String json = JSONUtil.toJsonStr(msg);
        synchronized (session) {
            session.sendMessage(new TextMessage(json));
        }
    }

    private void sendError(WebSocketSession session, String errMsg) throws IOException {
        SignalMessage err = new SignalMessage();
        err.setType("ERROR");
        err.setPayload(errMsg);
        sendMessage(session, err);
    }

    /** 信令消息 */
    public static class SignalMessage {
        private String type;           // OFFER, ANSWER, ICE_CANDIDATE, PEER_JOIN, PEER_LEAVE
        private String fromClientId;   // 发送方
        private String targetClientId; // 目标接收方（OFFER/ANSWER/ICE 时必填）
        private String sdp;            // SDP 内容（OFFER/ANSWER）
        private String candidate;      // ICE candidate
        private String sdpMid;         // ICE sdpMid
        private Integer sdpMLineIndex; // ICE sdpMLineIndex
        private String payload;        // 通用载荷

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getFromClientId() { return fromClientId; }
        public void setFromClientId(String id) { this.fromClientId = id; }
        public String getTargetClientId() { return targetClientId; }
        public void setTargetClientId(String id) { this.targetClientId = id; }
        public String getSdp() { return sdp; }
        public void setSdp(String sdp) { this.sdp = sdp; }
        public String getCandidate() { return candidate; }
        public void setCandidate(String candidate) { this.candidate = candidate; }
        public String getSdpMid() { return sdpMid; }
        public void setSdpMid(String mid) { this.sdpMid = mid; }
        public Integer getSdpMLineIndex() { return sdpMLineIndex; }
        public void setSdpMLineIndex(Integer idx) { this.sdpMLineIndex = idx; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
    }
}
