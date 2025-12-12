package com.dong.dongpicturebackend.manager.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author by hongdou
 * @date 2025/12/10.
 * @DESC: 客户端向服务端发送的图片编辑请求消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditRequestMessage {
    /**
     * 消息类型，例如ENTER_EDIT, EXIT_EDIT, EDIT_ACTION INFO ERROR
     */
    private String type;

    /**
     * 执行的编辑动作
     */
    private String editAction;
}
