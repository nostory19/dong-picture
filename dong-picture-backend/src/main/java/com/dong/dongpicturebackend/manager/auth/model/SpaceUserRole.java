package com.dong.dongpicturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author by hongdou
 * @date 2025/10/24.
 * @DESC: 空间成员角色
 */

@Data
public class SpaceUserRole implements Serializable {
    /**
     * 角色键
     */
    private String key;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 权限键列表
     */
    private List<String> permissions;

    /**
     * 角色描述
     */
    private String description;

    public static final long serialVersionUID = 1L;
}
