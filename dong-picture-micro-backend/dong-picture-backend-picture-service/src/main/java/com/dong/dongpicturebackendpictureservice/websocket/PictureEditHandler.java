package com.dong.dongpicturebackendpictureservice.websocket;

import cn.hutool.json.JSONUtil;
import com.dong.dongpicturebackendcollaborationservice.collab.engine.InMemoryOperationLog;
import com.dong.dongpicturebackendcollaborationservice.collab.engine.Operation;
import com.dong.dongpicturebackendcollaborationservice.collab.engine.OperationLog;
import com.dong.dongpicturebackendcollaborationservice.collab.engine.SyncProtocol;
import com.dong.dongpicturebackendpictureservice.websocket.disruptor.PictureEditEventProducer;
import com.dong.dongpicturebackendpictureservice.websocket.model.PictureEditActionEnum;
import com.dong.dongpicturebackendpictureservice.websocket.model.PictureEditMessageTypeEnum;
import com.dong.dongpicturebackendpictureservice.websocket.model.PictureEditRequestMessage;
import com.dong.dongpicturebackendpictureservice.websocket.model.PictureEditResponseMessage;
import com.dong.dongpicturebackendmodel.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.annotation.Resource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author by hongdou
 * @date 2025/12/11.
 * @DESC: 图片编辑websocket处理器
 */

// 定义为bean，方便后续注入
@Component
@Slf4j
public class PictureEditHandler extends TextWebSocketHandler {
    // 每张图片的编辑状态，key: pictureId, value:当前正在编辑的用户Id
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    // 保存所有连接的会话，key: pictureId, value: 用户会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    // TODO: 微服务迁移 - userService需要替换为UserFeignClient
    // @Resource
    // private UserService userService;

    @Resource
    @Lazy
    private PictureEditEventProducer pictureEditEventProducer;

    /** 协作引擎：操作日志（内存实现） */
    private final OperationLog operationLog = new InMemoryOperationLog();

    /** 协作引擎：同步协议（按 pictureId 获取或创建） */
    private final java.util.concurrent.ConcurrentHashMap<Long, SyncProtocol> syncProtocols = new java.util.concurrent.ConcurrentHashMap<>();

    private SyncProtocol getSyncProtocol(Long pictureId) {
        return syncProtocols.computeIfAbsent(pictureId, id -> new SyncProtocol(operationLog));
    }


    /**
     * 连接建立时调用
     *
     * @param session 是在握手时，从属性中获取的会话
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        // 保存会话到集合中
        // 会话中保存的形式为，例如：
        // pictureId: 123456, sessions: [session1, session2, session3]
        // 从属性中获取
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        // 添加会话到集合中
        pictureSessions.get(pictureId).add(session);
        // 构造响应，发送加入编辑的消息通知
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        // 设定为INFO类型
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        // 构造消息（user 可能为 null，微服务迁移中）
        String userName = (user != null) ? user.getUserName() : ("用户" + (user != null ? user.getId() : "未知"));
        String message = String.format("用户%s加入编辑", userName);
        pictureEditResponseMessage.setMessage(message);
        // TODO: 微服务迁移 - 使用UserFeignClient替换userService
        // 用户数据脱敏
        // pictureEditResponseMessage.setUser(userService.getUserVO(user));
        pictureEditResponseMessage.setUser(null);
        // 广播给同一张图片的用户
        broadcastToPicture(pictureId, pictureEditResponseMessage);

    }

    /**
     * 收到前端发送的编辑消息时调用
     *
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        // 将前端发送的JSON转换为PictureEditMessage对象
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);

        // 从session属性中获取公共参数
        Map<String, Object> attributes = session.getAttributes();
        // 获取当前登录用户
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");

        // 替换为disruptor事件发布.生产消息到disruptor环形队列中
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);
    }

    // 处理编辑动作消息，将编辑动作广播给其他用户，不包含当前用户
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        // 兜底：user 可能为 null（微服务迁移中）
        if (user == null) user = buildFallbackUser(session);
        // 获取当前用户和编辑动作
        Long editingUserId = pictureEditingUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) {
            return;
        }
        // 确认是当前编辑者
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String message = String.format("%s执行%s", user.getUserName(), actionEnum.getValue());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setEditAction(editAction);
            // TODO: 微服务迁移 - 使用UserFeignClient替换userService
            // pictureEditResponseMessage.setUser(userService.getUserVO(user));
            pictureEditResponseMessage.setUser(null);
            // 广播给同一张图片的用户
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        } else {
            // 非编辑者，返回错误提示
            PictureEditResponseMessage response = new PictureEditResponseMessage();
            response.setType(PictureEditMessageTypeEnum.ERROR.getValue());
            response.setMessage("当前您没有编辑权限，图片正在被其他用户编辑中");
            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(response)));
        }
    }

    // 退出编辑，移除当前用户编辑状态，然后广播退出编辑消息
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        if (user == null) user = buildFallbackUser(session);
        // 获取当前用户
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 移除当前用户的编辑状态
            pictureEditingUsers.remove(pictureId);
            // 构造响应
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            // 设定为INFO类型
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            // 构造消息
            String message = String.format("用户%s退出编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            // TODO: 微服务迁移 - 使用UserFeignClient替换userService
            // pictureEditResponseMessage.setUser(userService.getUserVO(user));
            pictureEditResponseMessage.setUser(null);
            // 广播给同一张图片的用户
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    // 设置当前用户为编辑用户，并向其他客户端发送消息
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        if (user == null) user = buildFallbackUser(session);
        // 没有用户正在编辑该图片才能进入编辑
        if (!pictureEditingUsers.containsKey(pictureId)) {
            // 设置当前用户为编辑用户
            pictureEditingUsers.put(pictureId, user.getId());
            // 然后广播消息
            // 构造响应，发送加入编辑的消息通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            // 设定为INFO类型
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            // 构造消息
            String message = String.format("用户%s开始编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            // TODO: 微服务迁移 - 使用UserFeignClient替换userService
            // pictureEditResponseMessage.setUser(userService.getUserVO(user));
            pictureEditResponseMessage.setUser(null);
            // 广播给同一张图片的用户
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        } else {
            // 锁被占用，返回错误提示给请求者
            PictureEditResponseMessage response = new PictureEditResponseMessage();
            response.setType(PictureEditMessageTypeEnum.ERROR.getValue());
            Long editingUserId = pictureEditingUsers.get(pictureId);
            response.setMessage(String.format("图片正在被用户(ID:%d)编辑中，请稍后再试", editingUserId));
            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(response)));
        }
    }

    /**
     * 连接关闭时调用，释放资源
     * 包含移除当前用户编辑状态，从集合中删除会话，发送通知
     *
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        // 退出是没有请求，所以参数为null
        handleExitEditMessage(null, session, user, pictureId);
        // 删除会话
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (sessionSet != null) {
            sessionSet.remove(session); // 从集合中删除会话
            if (sessionSet.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }
    }
    // alt + insert 重写方法

    /**
     * 向图片的所有会话广播消息，排除指定会话
     *
     * @param pictureId
     * @param pictureEditResponseMessage
     * @param excludeSession
     * @throws Exception
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage, WebSocketSession excludeSession) throws Exception {
        // 获取该图片的所有会话
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        // 使用Jackson
        ObjectMapper objectMapper = new ObjectMapper();
        // 配置序列化，将Long类型转化为String，解决丢失精度问题
        SimpleModule module = new SimpleModule();
        // 注册模块
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModule(module);
        // 序列化消息为字符串
        String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : sessionSet) {
            // 排除的会话
            if (excludeSession != null && excludeSession.equals(session)) {
                continue;
            }
            if (session.isOpen()) {
                session.sendMessage(textMessage);
            }
        }
    }

    // 全部广播
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws Exception {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }

    // ======== 新协议方法（CRDT 协作模式） ========

    /**
     * SYNC_STEP1：客户端请求缺失操作。
     * 客户端发来其本地状态向量，服务端计算并返回缺失的操作列表。
     */
    public void handleSyncStep1Message(PictureEditRequestMessage msg, WebSocketSession session,
                                        User user, Long pictureId) throws Exception {
        SyncProtocol protocol = getSyncProtocol(pictureId);
        java.util.Map<String, Long> clientSV = msg.getStateVector() != null
                ? msg.getStateVector() : java.util.Collections.emptyMap();
        String clientId = msg.getClientId() != null ? msg.getClientId() : session.getId();

        SyncProtocol.SyncStep1Result result = protocol.handleSyncStep1(pictureId, clientId, clientSV);

        PictureEditResponseMessage response = new PictureEditResponseMessage();
        response.setType(PictureEditMessageTypeEnum.SYNC_STEP1.getValue());
        response.setStateVector(result.getServerStateVector());
        response.setMissingOperations(opsToMaps(result.getMissingOperations()));
        response.setMessage("同步完成，收到 " + result.getMissingOperations().size() + " 条操作");
        session.sendMessage(new TextMessage(JSONUtil.toJsonStr(response)));
    }

    /**
     * SYNC_STEP2：客户端提交本地未同步操作。
     * 服务端分配全局序号，持久化，并广播给其他客户端。
     */
    public void handleSyncStep2Message(PictureEditRequestMessage msg, WebSocketSession session,
                                        User user, Long pictureId) throws Exception {
        SyncProtocol protocol = getSyncProtocol(pictureId);
        String clientId = msg.getClientId() != null ? msg.getClientId() : session.getId();

        java.util.List<Operation> ops = mapsToOps(msg.getPendingOps(), clientId, pictureId);
        SyncProtocol.SyncStep2Result result = protocol.handleSyncStep2(ops);

        // 回复 ACK
        PictureEditResponseMessage ack = new PictureEditResponseMessage();
        ack.setType(PictureEditMessageTypeEnum.SYNC_STEP2.getValue());
        ack.setAcknowledged(result.isAcknowledged());
        ack.setStateVector(result.getServerStateVector());
        ack.setAssignedSeqs(result.getAssignedSeqs());
        ack.setMessage("提交成功，分配序号: " + result.getAssignedSeqs());
        session.sendMessage(new TextMessage(JSONUtil.toJsonStr(ack)));

        // 广播新操作给其他客户端
        for (Operation op : ops) {
            PictureEditResponseMessage broadcast = new PictureEditResponseMessage();
            broadcast.setType(PictureEditMessageTypeEnum.OPERATION.getValue());
            broadcast.setEditAction(op.getField());
            broadcast.setMessage(op.getNewValue());
            broadcast.setStateVector(seqInfo(op.getSeq(), op.getClientId()));
            broadcastToPicture(pictureId, broadcast, session);
        }
    }

    /**
     * OPERATION：实时操作广播。
     */
    public void handleOperationMessage(PictureEditRequestMessage msg, WebSocketSession session,
                                        User user, Long pictureId) throws Exception {
        SyncProtocol protocol = getSyncProtocol(pictureId);
        String clientId = msg.getClientId() != null ? msg.getClientId() : session.getId();

        // 新协议使用 editAction 传递操作类型（ROTATE_LEFT/ROTATE_RIGHT/ZOOM_IN/ZOOM_OUT）
        String action = msg.getEditAction() != null ? msg.getEditAction()
                : (msg.getField() != null ? msg.getField() : "unknown");

        Operation op = new Operation();
        op.setClientId(clientId);
        op.setLamportClock(msg.getLamportClock() != null ? msg.getLamportClock() : 0);
        op.setPictureId(pictureId);
        op.setField(action);
        op.setNewValue(action);
        op.setOldValue("");

        Operation stored = protocol.processLiveOperation(op);

        // 广播编辑动作给其他客户端
        PictureEditResponseMessage broadcast = new PictureEditResponseMessage();
        broadcast.setType(PictureEditMessageTypeEnum.OPERATION.getValue());
        broadcast.setEditAction(action);
        broadcast.setMessage(String.format("用户%s执行%s", (user != null ? user.getUserName() : clientId), action));
        broadcast.setStateVector(seqInfo(stored.getSeq(), stored.getClientId()));
        broadcastToPicture(pictureId, broadcast, session);
    }

    /**
     * PRESENCE：更新并广播当前用户的在线状态。
     */
    public void handlePresenceMessage(PictureEditRequestMessage msg, WebSocketSession session,
                                       User user, Long pictureId) throws Exception {
        String clientId = msg.getClientId() != null ? msg.getClientId() : session.getId();
        String userName = user != null ? user.getUserName() : "用户" + clientId;

        PictureEditResponseMessage broadcast = new PictureEditResponseMessage();
        broadcast.setType(PictureEditMessageTypeEnum.PRESENCE.getValue());
        broadcast.setMessage(userName + " 编辑中");
        broadcast.setEditAction(msg.getEditingField());
        // 携带 Presence 信息
        java.util.Map<String, Object> presence = new java.util.HashMap<>();
        presence.put("clientId", clientId);
        presence.put("userName", userName);
        presence.put("editingField", msg.getEditingField());
        presence.put("cursorX", msg.getCursorX());
        presence.put("cursorY", msg.getCursorY());
        broadcast.setPresenceList(java.util.List.of(presence));
        broadcastToPicture(pictureId, broadcast, session);
    }

    /**
     * CURSOR：高频光标移动广播。
     */
    public void handleCursorMessage(PictureEditRequestMessage msg, WebSocketSession session,
                                     User user, Long pictureId) throws Exception {
        String clientId = msg.getClientId() != null ? msg.getClientId() : session.getId();

        PictureEditResponseMessage broadcast = new PictureEditResponseMessage();
        broadcast.setType(PictureEditMessageTypeEnum.CURSOR.getValue());
        broadcast.setEditAction("cursor");
        java.util.Map<String, Object> cursor = new java.util.HashMap<>();
        cursor.put("clientId", clientId);
        cursor.put("cursorX", msg.getCursorX());
        cursor.put("cursorY", msg.getCursorY());
        broadcast.setPresenceList(java.util.List.of(cursor));
        broadcastToPicture(pictureId, broadcast, session);
    }

    /**
     * CANVAS_FULL_SYNC：客户端请求全量同步（状态严重落后时使用）。
     */
    public void handleFullSyncMessage(PictureEditRequestMessage msg, WebSocketSession session,
                                       User user, Long pictureId) throws Exception {
        SyncProtocol protocol = getSyncProtocol(pictureId);
        String clientId = msg.getClientId() != null ? msg.getClientId() : session.getId();

        // 返回全量状态向量和所有操作
        java.util.Map<String, Long> emptySV = java.util.Collections.emptyMap();
        SyncProtocol.SyncStep1Result result = protocol.handleSyncStep1(pictureId, clientId, emptySV);

        PictureEditResponseMessage response = new PictureEditResponseMessage();
        response.setType(PictureEditMessageTypeEnum.CANVAS_FULL_SYNC.getValue());
        response.setStateVector(result.getServerStateVector());
        response.setMissingOperations(opsToMaps(result.getMissingOperations()));
        response.setMessage("全量同步完成，共 " + result.getMissingOperations().size() + " 条操作");
        session.sendMessage(new TextMessage(JSONUtil.toJsonStr(response)));
    }

    // ======== 辅助方法 ========

    private static java.util.List<java.util.Map<String, Object>> opsToMaps(java.util.List<Operation> ops) {
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (Operation op : ops) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
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

    private static java.util.List<Operation> mapsToOps(java.util.List<java.util.Map<String, Object>> maps,
                                                         String clientId, Long pictureId) {
        if (maps == null) return java.util.Collections.emptyList();
        java.util.List<Operation> ops = new java.util.ArrayList<>();
        for (java.util.Map<String, Object> map : maps) {
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

    private static java.util.Map<String, Long> seqInfo(long seq, String clientId) {
        java.util.Map<String, Long> map = new java.util.HashMap<>();
        map.put("seq", seq);
        map.put("clientId", Long.valueOf(clientId.hashCode()));
        return map;
    }

    /**
     * 构造兜底 User 对象（微服务迁移中，WsHandshakeInterceptor 暂不解析用户）。
     */
    private User buildFallbackUser(WebSocketSession session) {
        User fallback = new User();
        Object userId = session.getAttributes().get("userId");
        if (userId != null) {
            fallback.setId(Long.valueOf(userId.toString()));
        } else {
            fallback.setId(0L);
        }
        fallback.setUserName("用户" + fallback.getId());
        return fallback;
    }

    /**
     * 检查是否是新协议消息类型。
     */
    public static boolean isNewProtocolMessage(String type) {
        return "SYNC_STEP1".equals(type) || "SYNC_STEP2".equals(type)
                || "OPERATION".equals(type) || "PRESENCE".equals(type)
                || "CURSOR".equals(type) || "CANVAS_FULL_SYNC".equals(type);
    }

}
