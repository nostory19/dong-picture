package com.dong.dongpicturebackendspaceservice.auth.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author by hongdou
 * @date 2025/10/24.
 * @DESC: 空间成员权限
 */

@Data
public class SpaceUserPermission implements Serializable {
    /**
     * 权限键
     */
    private String key;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限描述
     */
    private String description;

    public static final long serialVersionUID = 1L;
}
