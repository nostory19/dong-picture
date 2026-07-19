package com.dong.dongpicturebackendcollaborationservice.collab.network;

import java.util.List;
import java.util.Map;

/**
 * 协作协议消息 — 所有客户端↔服务端通信的统一消息格式。
 */
public class CollabMessage {

    /** 消息类型 */
    private String type;

    /** 客户端 ID（首次连接时由客户端生成并持久化到 localStorage） */
    private String clientId;

    /** 图片 ID */
    private Long pictureId;

    // === 同步消息字段 ===

    /** 客户端状态向量（SYNC_STEP1 时携带） */
    private Map<String, Long> stateVector;

    /** 客户端未同步的操作列表（SYNC_STEP2 时携带） */
    private List<Map<String, Object>> pendingOps;

    // === 操作消息字段 ===

    /** 操作字段名 */
    private String field;

    /** 新值 */
    private String newValue;

    /** 旧值 */
    private String oldValue;

    /** 客户端 Lamport 时钟 */
    private Long lamportClock;

    // === Presence 消息字段 ===

    /** 正在编辑的字段 */
    private String editingField;

    /** 光标 X */
    private Double cursorX;

    /** 光标 Y */
    private Double cursorY;

    /** 选中区域 JSON */
    private String selection;

    /** 用户名称 */
    private String userName;

    /** 用户头像 URL */
    private String userAvatar;

    // === 服务端响应字段 ===

    /** 服务端状态向量 */
    private Map<String, Long> serverStateVector;

    /** 缺失的操作列表（SYNC_STEP1 响应） */
    private List<Map<String, Object>> missingOps;

    /** 分配的序号列表（SYNC_STEP2 响应） */
    private List<Long> assignedSeqs;

    /** 在线用户列表 */
    private List<Map<String, Object>> onlineClients;

    /** 错误/提示消息 */
    private String message;

    /** ACK 确认 */
    private Boolean acknowledged;

    // === Getters & Setters ===

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public Long getPictureId() { return pictureId; }
    public void setPictureId(Long pictureId) { this.pictureId = pictureId; }
    public Map<String, Long> getStateVector() { return stateVector; }
    public void setStateVector(Map<String, Long> stateVector) { this.stateVector = stateVector; }
    public List<Map<String, Object>> getPendingOps() { return pendingOps; }
    public void setPendingOps(List<Map<String, Object>> pendingOps) { this.pendingOps = pendingOps; }
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    public Long getLamportClock() { return lamportClock; }
    public void setLamportClock(Long lamportClock) { this.lamportClock = lamportClock; }
    public String getEditingField() { return editingField; }
    public void setEditingField(String editingField) { this.editingField = editingField; }
    public Double getCursorX() { return cursorX; }
    public void setCursorX(Double cursorX) { this.cursorX = cursorX; }
    public Double getCursorY() { return cursorY; }
    public void setCursorY(Double cursorY) { this.cursorY = cursorY; }
    public String getSelection() { return selection; }
    public void setSelection(String selection) { this.selection = selection; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    public Map<String, Long> getServerStateVector() { return serverStateVector; }
    public void setServerStateVector(Map<String, Long> serverStateVector) { this.serverStateVector = serverStateVector; }
    public List<Map<String, Object>> getMissingOps() { return missingOps; }
    public void setMissingOps(List<Map<String, Object>> missingOps) { this.missingOps = missingOps; }
    public List<Long> getAssignedSeqs() { return assignedSeqs; }
    public void setAssignedSeqs(List<Long> assignedSeqs) { this.assignedSeqs = assignedSeqs; }
    public List<Map<String, Object>> getOnlineClients() { return onlineClients; }
    public void setOnlineClients(List<Map<String, Object>> onlineClients) { this.onlineClients = onlineClients; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Boolean getAcknowledged() { return acknowledged; }
    public void setAcknowledged(Boolean acknowledged) { this.acknowledged = acknowledged; }
}
