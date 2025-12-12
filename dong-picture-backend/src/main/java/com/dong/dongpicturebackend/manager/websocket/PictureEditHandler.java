package com.dong.dongpicturebackend.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.dong.dongpicturebackend.manager.websocket.disruptor.PictureEditEventProducer;
import com.dong.dongpicturebackend.manager.websocket.model.PictureEditActionEnum;
import com.dong.dongpicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.dong.dongpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.dong.dongpicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.dong.dongpicturebackend.model.entity.User;
import com.dong.dongpicturebackend.service.UserService;
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

import javax.annotation.Resource;
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

    @Resource
    private UserService userService;
    @Resource
    @Lazy
    private PictureEditEventProducer pictureEditEventProducer;


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
        // 构造消息
        String message = String.format("用户%s加入编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        // 用户数据脱敏
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
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
        // 使用disruptor，会话也就不需要保存了
//        String type = pictureEditRequestMessage.getType();
//        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.valueOf(type);

        // 从session属性中获取公共参数
        Map<String, Object> attributes = session.getAttributes();
        // 获取当前登录用户
        User user = (User) attributes.get("user");
//        Long userId = (Long) attributes.get("userId");
        Long pictureId = (Long) attributes.get("pictureId");

//        // 调用对应的消息处理方法
//        switch(pictureEditMessageTypeEnum) {
//            case ENTER_EDIT:
//                handleEnterEditMessage(pictureEditRequestMessage, session ,user, pictureId);
//                break;
//            case EXIT_EDIT:
//                handleExitEditMessage(pictureEditRequestMessage, session, user, pictureId);
//                break;
//            case EDIT_ACTION:
//                handleEditActionMessage(pictureEditRequestMessage, session, user, pictureId);
//                break;
//            default:
//                // 说明是其他类型的消息，返回错误响应
//                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
//                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
//                pictureEditResponseMessage.setMessage("消息类型错误");
//                pictureEditResponseMessage.setUser(userService.getUserVO(user));
//                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));
//        }

        // 替换为disruptor事件发布.生产消息到disruptor环形队列中
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);
    }

    // 处理编辑动作消息，将编辑动作广播给其他用户，不包含当前用户
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
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
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            // 广播给同一张图片的用户
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }

    // 退出编辑，移除当前用户编辑状态，然后广播退出编辑消息
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
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
            // 用户数据脱敏
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            // 广播给同一张图片的用户
//            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    // 设置当前用户为编辑用户，并向其他客户端发送消息
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
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
            // 用户数据脱敏
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            // 广播给同一张图片的用户
            broadcastToPicture(pictureId, pictureEditResponseMessage);
            // 这里也写错了， 多传了session， 应该只传pictureEditResponseMessage
//            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
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
        Set<WebSocketSession> sessionSet = pictureSessions.get("pictureId");
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
//        if (ObjUtil.isNotEmpty(sessionSet)) {
//            // 遍历会话，排除指定会话
//            for (WebSocketSession webSocketSession : sessionSet) {
//                if (!webSocketSession.equals(excludeSession)) {
//                    webSocketSession.sendMessage(new TextMessage(pictureEditResponseMessage.toString()));
//                }
//            }
//        }
        // 上述写法是有坑的，因为pictureEditResponseMessage里面包含的用户信息的id是long类型的，
        // 前端js是不能直接解析long类型的id，会丢失精度，所以这里需要转换为字符串
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


}
