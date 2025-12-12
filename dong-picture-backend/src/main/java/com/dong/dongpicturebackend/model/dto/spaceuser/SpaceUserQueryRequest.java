package com.dong.dongpicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * @author by hongdou
 * @date 2025/10/16.
 * @DESC: 查询空间成员请求，可以不用分页
 */
@Data
public class SpaceUserQueryRequest implements Serializable {
    /**
     * ID
     */
    private Long id;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 空间角色：viewer-浏览者，editor-编辑者，admin-管理员
     */
    private String spaceRole;


    public static final long serialVersionUID = 1L;
}
