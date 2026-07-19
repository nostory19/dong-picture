package com.dong.dongpicturebackendpictureservice.websocket.model;

import com.dong.dongpicturebackendmodel.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 服务端向客户端发送的图片编辑响应消息（兼容新旧协议）。
 *
 * 旧协议字段：
 *   type, message, editAction, user
 *
 * 新协议字段（CRDT 协作模式）：
 *   stateVector, missingOperations, presenceList, assignedSeqs, onlineClients
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditResponseMessage {

    /** 消息类型 */
    private String type;

    /** 信息/错误描述 */
    private String message;

    // === 旧协议字段 ===

    /** 执行的编辑动作 */
    private String editAction;

    /** 用户信息 */
    private UserVO user;

    // === 新协议字段 ===

    /** 服务端状态向量 { clientId → lastSeq } */
    private Map<String, Long> stateVector;

    /** 缺失的操作列表（SYNC_STEP1 响应） */
    private List<Map<String, Object>> missingOperations;

    /** 分配的全局序号列表（SYNC_STEP2 响应） */
    private List<Long> assignedSeqs;

    /** 在线用户 Presence 列表 */
    private List<Map<String, Object>> presenceList;

    /** 在线客户端列表 */
    private List<Map<String, Object>> onlineClients;

    /** ACK 确认（SYNC_STEP2 响应） */
    private Boolean acknowledged;
}
