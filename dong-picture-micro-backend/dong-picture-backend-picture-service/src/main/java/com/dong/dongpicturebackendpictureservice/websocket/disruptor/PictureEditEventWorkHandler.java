package com.dong.dongpicturebackendpictureservice.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.dong.dongpicturebackendpictureservice.websocket.PictureEditHandler;
import com.dong.dongpicturebackendpictureservice.websocket.model.PictureEditMessageTypeEnum;
import com.dong.dongpicturebackendpictureservice.websocket.model.PictureEditRequestMessage;
import com.dong.dongpicturebackendpictureservice.websocket.model.PictureEditResponseMessage;
import com.dong.dongpicturebackendmodel.entity.User;
import com.lmax.disruptor.WorkHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import jakarta.annotation.Resource;

/**
 * @author by hongdou
 * @date 2025/12/11.
 * @DESC:
 */

@Slf4j
@Component
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    private PictureEditHandler pictureEditHandler;

    // TODO: 微服务迁移 - userService需要替换为UserFeignClient
    // @Resource
    // private UserService userService;

    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        PictureEditRequestMessage pictureEditRequestMessage = pictureEditEvent.getPictureEditRequestMessage();
        WebSocketSession session = pictureEditEvent.getSession();
        User user = pictureEditEvent.getUser();
        Long pictureId = pictureEditEvent.getPictureId();
        // 获取到消息类别（兼容新旧协议，使用 getEnumByValue 而非 valueOf）
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);
        if (pictureEditMessageTypeEnum == null) {
            PictureEditResponseMessage picErr = new PictureEditResponseMessage();
            picErr.setType(PictureEditMessageTypeEnum.ERROR.getValue());
            picErr.setMessage("未知消息类型: " + type);
            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(picErr)));
            return;
        }
        // 调用对应的消息处理方法
        switch (pictureEditMessageTypeEnum) {
            // === 旧协议（独占编辑） — 向后兼容 ===
            case ENTER_EDIT:
                pictureEditHandler.handleEnterEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EXIT_EDIT:
                pictureEditHandler.handleExitEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EDIT_ACTION:
                pictureEditHandler.handleEditActionMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            // === 新协议（CRDT 协作） ===
            case SYNC_STEP1:
                pictureEditHandler.handleSyncStep1Message(pictureEditRequestMessage, session, user, pictureId);
                break;
            case SYNC_STEP2:
                pictureEditHandler.handleSyncStep2Message(pictureEditRequestMessage, session, user, pictureId);
                break;
            case OPERATION:
                pictureEditHandler.handleOperationMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case PRESENCE:
                pictureEditHandler.handlePresenceMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case CURSOR:
                pictureEditHandler.handleCursorMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case CANVAS_FULL_SYNC:
                pictureEditHandler.handleFullSyncMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case HEARTBEAT:
                // 心跳消息，无需处理
                break;
            default:
                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                pictureEditResponseMessage.setMessage("消息类型错误");
                // TODO: 微服务迁移 - 使用UserFeignClient替换userService
                // pictureEditResponseMessage.setUser(userService.getUserVO(user));
                pictureEditResponseMessage.setUser(null);
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));
        }
    }
}
