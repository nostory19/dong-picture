package com.dong.dongpicturebackendcollaborationservice.collab.network;

/**
 * 协作消息类型枚举。
 */
public final class CollabMessageType {

    private CollabMessageType() {}

    /** 三阶段同步 Step1：客户端请求同步 */
    public static final String SYNC_STEP1 = "SYNC_STEP1";

    /** 三阶段同步 Step2：客户端提交本地操作 */
    public static final String SYNC_STEP2 = "SYNC_STEP2";

    /** 实时操作 */
    public static final String OPERATION = "OPERATION";

    /** Presence 更新（编辑字段、光标位置） */
    public static final String PRESENCE = "PRESENCE";

    /** 心跳 */
    public static final String HEARTBEAT = "HEARTBEAT";

    /** 服务端 → 客户端：信息通知 */
    public static final String INFO = "INFO";

    /** 服务端 → 客户端：错误 */
    public static final String ERROR = "ERROR";

    /** 服务端 → 客户端：在线用户变更 */
    public static final String CLIENT_JOIN = "CLIENT_JOIN";

    /** 服务端 → 客户端：用户离开 */
    public static final String CLIENT_LEAVE = "CLIENT_LEAVE";
}
