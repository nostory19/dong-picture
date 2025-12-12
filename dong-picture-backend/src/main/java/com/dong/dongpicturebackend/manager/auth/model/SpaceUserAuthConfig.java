package com.dong.dongpicturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author by hongdou
 * @date 2025/10/24.
 * @DESC: 空间成员权限配置
 */
@Data
public class SpaceUserAuthConfig implements Serializable {
    /**
     * 权限列表
     */
    private List<SpaceUserPermission> permissions;

    /**
     * 角色列表
     */
    private List<SpaceUserRole> roles;

    public static final long serialVersionUID = 1L;
}
