package com.dong.dongpicturebackendpictureservice.websocket.model;

import lombok.Getter;

/**
 * @author by hongdou
 * @date 2025/12/10.
 * @DESC: 图片编辑消息类型枚举
 */

@Getter
public enum PictureEditMessageTypeEnum {

    INFO("发送通知", "INFO"),
    ERROR("发送错误", "ERROR"),

    // === 旧协议（独占编辑模式，向后兼容） ===
    ENTER_EDIT("进入编辑状态", "ENTER_EDIT"),
    EXIT_EDIT("退出编辑状态", "EXIT_EDIT"),
    EDIT_ACTION("执行编辑操作", "EDIT_ACTION"),

    // === 新协议（CRDT 协作模式） ===
    /** 三阶段同步 Step1：客户端请求缺失操作 */
    SYNC_STEP1("同步步骤1", "SYNC_STEP1"),
    /** 三阶段同步 Step2：客户端提交本地未同步操作 */
    SYNC_STEP2("同步步骤2", "SYNC_STEP2"),
    /** 实时操作广播 */
    OPERATION("协作操作", "OPERATION"),
    /** Presence 更新（编辑字段、光标位置） */
    PRESENCE("在线状态", "PRESENCE"),
    /** 光标移动（高频，走 WebRTC 或 WebSocket） */
    CURSOR("光标移动", "CURSOR"),
    /** 全量同步请求（客户端状态严重落后时） */
    CANVAS_FULL_SYNC("全量同步", "CANVAS_FULL_SYNC"),
    /** 心跳保活 */
    HEARTBEAT("心跳", "HEARTBEAT");

    private final String text;
    private final String value;

    PictureEditMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value获取枚举
     *
     */
    public static PictureEditMessageTypeEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (PictureEditMessageTypeEnum typeEnum : values()) {
            if (typeEnum.getValue().equals(value)) {
                return typeEnum;
            }
        }
        return null;
    }
}
