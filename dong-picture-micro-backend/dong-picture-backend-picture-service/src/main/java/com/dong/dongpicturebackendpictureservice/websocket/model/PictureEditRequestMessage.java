package com.dong.dongpicturebackendpictureservice.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 客户端向服务端发送的图片编辑请求消息（兼容新旧协议）。
 *
 * 旧协议字段（独占编辑模式）：
 *   type, editAction
 *
 * 新协议字段（CRDT 协作模式）：
 *   clientId, lamportClock, stateVector, field, value, pendingOps
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditRequestMessage {

    /** 消息类型 */
    private String type;

    // === 旧协议字段 ===

    /** 执行的编辑动作 (ZOOM_IN, ZOOM_OUT, ROTATE_LEFT, ROTATE_RIGHT) */
    private String editAction;

    // === 新协议字段 ===

    /** 客户端唯一 ID（持久化到 localStorage 的 UUID） */
    private String clientId;

    /** 客户端 Lamport 逻辑时钟值 */
    private Long lamportClock;

    /** 客户端状态向量 { clientId → lastSeq } */
    private Map<String, Long> stateVector;

    /** 操作的属性字段名 */
    private String field;

    /** 操作后的新值 */
    private String value;

    /** 操作前的旧值 */
    private String oldValue;

    /** 待提交的离线操作列表（SYNC_STEP2 时携带） */
    private List<Map<String, Object>> pendingOps;

    /** 正在编辑的字段（PRESENCE 时携带） */
    private String editingField;

    /** 光标 X 坐标 */
    private Double cursorX;

    /** 光标 Y 坐标 */
    private Double cursorY;
}
