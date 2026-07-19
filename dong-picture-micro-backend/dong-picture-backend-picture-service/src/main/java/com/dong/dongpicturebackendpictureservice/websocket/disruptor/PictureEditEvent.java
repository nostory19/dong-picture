package com.dong.dongpicturebackendpictureservice.websocket.disruptor;

import com.dong.dongpicturebackendpictureservice.websocket.model.PictureEditRequestMessage;
import com.dong.dongpicturebackendmodel.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * @author by hongdou
 * @date 2025/12/11.
 * @DESC: 图片编辑事件
 */
@Data
public class PictureEditEvent {
    /**
     * 图片编辑请求消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 图片编辑请求消息对应的WebSocket会话
     */
    private WebSocketSession session;

    /**
     * 图片编辑请求消息对应的用户
     */
    private User user;


    /**
     * 图片编辑请求消息对应的图片id
     */
    private Long pictureId;
}
