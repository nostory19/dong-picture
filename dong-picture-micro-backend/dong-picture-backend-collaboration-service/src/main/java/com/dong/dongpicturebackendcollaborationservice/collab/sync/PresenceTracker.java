package com.dong.dongpicturebackendcollaborationservice.collab.sync;

import com.dong.dongpicturebackendcollaborationservice.collab.engine.CRDTDocument;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线用户状态追踪。
 *
 * 维护每张图片的在线用户 Presence 信息：
 * - 谁在线
 * - 正在编辑哪个属性字段
 * - 光标位置
 * - 最后一次心跳时间
 */
public class PresenceTracker {

    /** pictureId → (clientId → presence) */
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, CRDTDocument.ClientPresence>> presences = new ConcurrentHashMap<>();

    /** pictureId → (clientId → lastHeartbeatMs) */
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, Long>> heartbeats = new ConcurrentHashMap<>();

    /**
     * 用户加入编辑房间
     */
    public void join(Long pictureId, String clientId, CRDTDocument.ClientPresence presence) {
        presences.computeIfAbsent(pictureId, k -> new ConcurrentHashMap<>())
                .put(clientId, presence);
        heartbeats.computeIfAbsent(pictureId, k -> new ConcurrentHashMap<>())
                .put(clientId, System.currentTimeMillis());
    }

    /**
     * 用户离开编辑房间
     */
    public void leave(Long pictureId, String clientId) {
        ConcurrentHashMap<String, CRDTDocument.ClientPresence> room = presences.get(pictureId);
        if (room != null) {
            room.remove(clientId);
            if (room.isEmpty()) presences.remove(pictureId);
        }
        ConcurrentHashMap<String, Long> hb = heartbeats.get(pictureId);
        if (hb != null) {
            hb.remove(clientId);
            if (hb.isEmpty()) heartbeats.remove(pictureId);
        }
    }

    /**
     * 更新用户的编辑状态（正在编辑的字段、光标位置等）
     */
    public void updatePresence(Long pictureId, String clientId,
                               String editingField, double cursorX, double cursorY, String selection) {
        CRDTDocument.ClientPresence p = getPresence(pictureId, clientId);
        if (p != null) {
            p.setEditingField(editingField);
            p.setCursorX(cursorX);
            p.setCursorY(cursorY);
            p.setSelection(selection);
        }
        // 更新心跳
        ConcurrentHashMap<String, Long> hb = heartbeats.get(pictureId);
        if (hb != null) {
            hb.put(clientId, System.currentTimeMillis());
        }
    }

    /**
     * 获取指定用户的 Presence
     */
    public CRDTDocument.ClientPresence getPresence(Long pictureId, String clientId) {
        ConcurrentHashMap<String, CRDTDocument.ClientPresence> room = presences.get(pictureId);
        return room != null ? room.get(clientId) : null;
    }

    /**
     * 获取指定图片的所有在线用户
     */
    public List<CRDTDocument.ClientPresence> getOnlineClients(Long pictureId) {
        ConcurrentHashMap<String, CRDTDocument.ClientPresence> room = presences.get(pictureId);
        return room != null ? List.copyOf(room.values()) : Collections.emptyList();
    }

    /**
     * 获取在线人数
     */
    public int getOnlineCount(Long pictureId) {
        ConcurrentHashMap<String, CRDTDocument.ClientPresence> room = presences.get(pictureId);
        return room != null ? room.size() : 0;
    }

    /**
     * 心跳超时清理：移除超过 timeoutMs 未心跳的用户
     *
     * @return 被清理的 (pictureId, clientId) 列表
     */
    public List<Map.Entry<Long, String>> evictStaleSessions(long timeoutMs) {
        long now = System.currentTimeMillis();
        List<Map.Entry<Long, String>> evicted = new ArrayList<>();
        heartbeats.forEach((pictureId, room) -> {
            Iterator<Map.Entry<String, Long>> it = room.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> entry = it.next();
                if (now - entry.getValue() > timeoutMs) {
                    evicted.add(new AbstractMap.SimpleEntry<>(pictureId, entry.getKey()));
                    it.remove();
                    ConcurrentHashMap<String, CRDTDocument.ClientPresence> p = presences.get(pictureId);
                    if (p != null) p.remove(entry.getKey());
                }
            }
            if (room.isEmpty()) heartbeats.remove(pictureId);
        });
        return evicted;
    }
}
