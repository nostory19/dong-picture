package com.dong.dongpicturebackendcollaborationservice.collab.engine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CRDT 文档 — Relay 模式下的服务端轻量文档。
 *
 * 在 Server Relay 模式下，服务端不解析 CRDT 内部状态，
 * 只做：操作序列管理 + 状态向量追踪 + 快照引用。
 * 真正的 CRDT merge (Yjs WASM) 在客户端执行。
 *
 * 这张图的所有编辑状态被抽象为一个有序操作序列：
 *   op1(clientA, rotate=45) → op2(clientB, scale=1.2) → op3(clientA, cropX=100) → ...
 *
 * 新客户端加入时，服务端按状态向量返回缺失的操作，客户端重放即可同步。
 */
public class CRDTDocument {

    private final Long pictureId;
    private final LamportClock documentClock;
    private final StateVector documentStateVector;

    /** 当前在线编辑的客户端 ID 集合 */
    private final ConcurrentHashMap<String, ClientPresence> presenceMap;

    /** 最近一次快照的序号 */
    private volatile long lastSnapshotSeq;

    public CRDTDocument(Long pictureId) {
        this.pictureId = pictureId;
        this.documentClock = new LamportClock();
        this.documentStateVector = new StateVector();
        this.presenceMap = new ConcurrentHashMap<>();
        this.lastSnapshotSeq = 0;
    }

    public Long getPictureId() {
        return pictureId;
    }

    public LamportClock getClock() {
        return documentClock;
    }

    public StateVector getStateVector() {
        return documentStateVector;
    }

    // === Presence ===

    public void addPresence(String clientId, ClientPresence presence) {
        presenceMap.put(clientId, presence);
    }

    public void removePresence(String clientId) {
        presenceMap.remove(clientId);
    }

    public Map<String, ClientPresence> getPresenceMap() {
        return presenceMap;
    }

    public List<ClientPresence> getOnlineClients() {
        return List.copyOf(presenceMap.values());
    }

    // === Snapshot ===

    public long getLastSnapshotSeq() {
        return lastSnapshotSeq;
    }

    public void setLastSnapshotSeq(long seq) {
        this.lastSnapshotSeq = seq;
    }

    /**
     * 客户端在线状态
     */
    public static class ClientPresence {
        private String clientId;
        private Long userId;
        private String userName;
        private String userAvatar;
        /** 正在编辑的属性字段名（可为 null 表示仅查看） */
        private String editingField;
        /** 光标位置 X */
        private double cursorX;
        /** 光标位置 Y */
        private double cursorY;
        /** 选择的区域（JSON） */
        private String selection;

        public ClientPresence() {}

        public ClientPresence(String clientId, Long userId, String userName) {
            this.clientId = clientId;
            this.userId = userId;
            this.userName = userName;
        }

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getUserAvatar() { return userAvatar; }
        public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
        public String getEditingField() { return editingField; }
        public void setEditingField(String editingField) { this.editingField = editingField; }
        public double getCursorX() { return cursorX; }
        public void setCursorX(double cursorX) { this.cursorX = cursorX; }
        public double getCursorY() { return cursorY; }
        public void setCursorY(double cursorY) { this.cursorY = cursorY; }
        public String getSelection() { return selection; }
        public void setSelection(String selection) { this.selection = selection; }
    }
}
