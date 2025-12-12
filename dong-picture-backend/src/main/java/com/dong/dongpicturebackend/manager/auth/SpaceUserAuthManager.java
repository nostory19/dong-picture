package com.dong.dongpicturebackend.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.dong.dongpicturebackend.manager.auth.model.SpaceUserAuthConfig;
import com.dong.dongpicturebackend.manager.auth.model.SpaceUserRole;
import com.dong.dongpicturebackend.model.entity.Space;
import com.dong.dongpicturebackend.model.entity.SpaceUser;
import com.dong.dongpicturebackend.model.entity.User;
import com.dong.dongpicturebackend.model.enums.SpaceRoleEnum;
import com.dong.dongpicturebackend.model.enums.SpaceTypeEnum;
import com.dong.dongpicturebackend.service.SpaceUserService;
import com.dong.dongpicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author by hongdou
 * @date 2025/10/24.
 * @DESC: 空间成员权限管理
 */

@Component
public class SpaceUserAuthManager {

    @Resource
    private UserService userService;

    // 定义一个全局变量读取json文件
    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        // 项目启动时就读取了
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        // 将读取到的json转换成需要的类
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    @Resource
    private SpaceUserService spaceUserService;


    // 由于用户表中只有角色，因此需要根据角色去查找权限
    public List<String> getPermissionsByRole(String spaceUserRole) {
        if (StrUtil.isBlank(spaceUserRole)) {
            return new ArrayList<>();
        }
        // 找到匹配的角色
        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles()
                .stream()
                .filter(r -> spaceUserRole.equals(r.getKey()))
                .findFirst()
                .orElse(null);
        if (role == null) {
            return new ArrayList<>();
        }
        // 返回该角色的权限
        return role.getPermissions();
    }

    /**
     * 获取用户在空间中的权限列表
     *
     * @param space
     * @param loginUser
     * @return
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }
        // 管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库
        if (space == null) {
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
            return new ArrayList<>();
        }
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }
        // 根据空间获取对应的权限
        switch (spaceTypeEnum) {
            case PRIVATE:
                // 私有空间，仅本人或管理员有所有权限
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                // 团队空间，查询 SpaceUser 并获取角色和权限
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                } else {
                    return getPermissionsByRole(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }
}
