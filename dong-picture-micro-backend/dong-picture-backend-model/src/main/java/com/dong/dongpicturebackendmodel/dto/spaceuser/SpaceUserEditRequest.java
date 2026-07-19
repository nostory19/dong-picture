package com.dong.dongpicturebackendmodel.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * @author by hongdou
 * @date 2025/10/16.
 * @DESC: 编辑空间成员请求，给空间管理员使用
 * 可以设置空间成员的角色
 */
@Data
public class SpaceUserEditRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 空间角色
     */
    private String spaceRole;

    public static final long serialVersionUID = 1L;
}