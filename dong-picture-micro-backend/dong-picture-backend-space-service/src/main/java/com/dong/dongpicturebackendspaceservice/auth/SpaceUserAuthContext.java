package com.dong.dongpicturebackendspaceservice.auth;

import com.dong.dongpicturebackendmodel.entity.Picture;
import com.dong.dongpicturebackendmodel.entity.Space;
import com.dong.dongpicturebackendmodel.entity.SpaceUser;
import lombok.Data;

/**
 * @author by hongdou
 * @date 2025/10/24.
 * @DESC: 表示用户在特定空间内的授权上下文，包括关联的图片、空间和用户信息
 */
@Data
public class SpaceUserAuthContext {
    /**
     * 临时参数，不同请求对应的id可能不同
     */
    private Long id;

    /**
     * 图片id
     */
    private Long pictureId;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 空间用户id
     */
    private Long spaceUserId;

    /**
     * 图片信息
     */
    private Picture picture;

    /**
     * 空间信息
     */
    private Space space;

    /**
     * 空间用户信息
     */
    private SpaceUser spaceUser;
}
