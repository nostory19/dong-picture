package com.dong.dongpicturebackendcollaborationservice.collab.sync;

import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协作会话管理 — 维护图片 ID 与 WebSocket 会话的映射关系。
 *
 * 对每张图片，跟踪所有已连接的客户端会话，
 * 广播操作时遍历这些会话发送消息。
 */
public class CollaborationSession {

    /** pictureId → 活跃的 WebSocket 会话集合 */
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    /**
     * 注册会话到指定图片的房间
     */
    public void join(Long pictureId, WebSocketSession session) {
        pictureSessions.computeIfAbsent(pictureId, k -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    /**
     * 从指定图片的房间移除会话
     */
    public void leave(Long pictureId, WebSocketSession session) {
        Set<WebSocketSession> sessions = pictureSessions.get(pictureId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }
    }

    /**
     * 获取指定图片的所有活跃会话
     */
    public Set<WebSocketSession> getSessions(Long pictureId) {
        return pictureSessions.getOrDefault(pictureId, Set.of());
    }

    /**
     * 获取某张图片的在线人数
     */
    public int getSessionCount(Long pictureId) {
        Set<WebSocketSession> sessions = pictureSessions.get(pictureId);
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * 会话关闭时的清理：遍历所有房间移除该会话
     */
    public void removeSessionGlobally(WebSocketSession session) {
        pictureSessions.forEach((pictureId, sessions) -> {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        });
    }

    /**
     * 获取所有活跃的房间 ID
     */
    public Set<Long> getActiveRoomIds() {
        return pictureSessions.keySet();
    }
}
