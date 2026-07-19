package com.dong.dongpicturebackendspaceservice.interfaces.controller.inner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dong.dongpicturebackendcommon.common.BaseResponse;
import com.dong.dongpicturebackendcommon.common.ResultUtils;
import com.dong.dongpicturebackendmodel.entity.Space;
import com.dong.dongpicturebackendmodel.entity.SpaceUser;
import com.dong.dongpicturebackendmodel.vo.SpaceVO;
import com.dong.dongpicturebackendspaceservice.auth.SpaceUserAuthManager;
import com.dong.dongpicturebackendspaceservice.auth.model.SpaceUserPermissionConstant;
import com.dong.dongpicturebackendspaceservice.domain.space.service.SpaceService;
import com.dong.dongpicturebackendspaceservice.domain.space.service.SpaceUserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 空间内部 Feign 接口 — 供图片服务调用。
 * 路径 /inner/* 被网关拦截，仅内部服务可访问。
 */
@RestController
@RequestMapping("/inner")
public class SpaceInnerController {

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @GetMapping("/getById")
    public BaseResponse<Space> getSpaceById(@RequestParam("spaceId") Long spaceId) {
        Space space = spaceService.getById(spaceId);
        return ResultUtils.success(space);
    }

    @GetMapping("/getVOById")
    public BaseResponse<SpaceVO> getSpaceVOById(@RequestParam("spaceId") Long spaceId) {
        Space space = spaceService.getById(spaceId);
        SpaceVO spaceVO = spaceService.getSpaceVO(space);
        return ResultUtils.success(spaceVO);
    }

    /**
     * 检查用户是否有指定空间的操作权限。
     * 私有空间：仅空间所有者
     * 团队空间：检查 space_user 表，按角色（viewer/editor/admin）分配权限
     */
    @PostMapping("/checkPermission")
    public BaseResponse<Boolean> checkPermission(@RequestParam("spaceId") Long spaceId,
                                                  @RequestParam("userId") Long userId,
                                                  @RequestParam("permission") String permission) {
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            return ResultUtils.success(false);
        }
        // 空间所有者拥有全部权限
        if (space.getUserId().equals(userId)) {
            return ResultUtils.success(true);
        }
        // 查询用户是否为空间成员
        SpaceUser spaceUser = spaceUserService.getOne(
                new LambdaQueryWrapper<SpaceUser>()
                        .eq(SpaceUser::getSpaceId, spaceId)
                        .eq(SpaceUser::getUserId, userId)
        );
        if (spaceUser == null) {
            return ResultUtils.success(false);
        }
        // 根据角色判断权限
        List<String> permissions = spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        return ResultUtils.success(permissions.contains(permission));
    }

    /**
     * 增加空间使用量（图片上传后调用）
     */
    @PostMapping("/increaseUsage")
    public BaseResponse<Boolean> increaseUsage(@RequestParam("spaceId") Long spaceId,
                                                @RequestParam("size") Long size,
                                                @RequestParam("count") Integer count) {
        boolean result = spaceService.lambdaUpdate()
                .eq(Space::getId, spaceId)
                .setSql("totalSize = totalSize + " + size)
                .setSql("totalCount = totalCount + " + count)
                .update();
        return ResultUtils.success(result);
    }

    /**
     * 获取用户在空间中的权限列表
     */
    @PostMapping("/getPermissionList")
    public BaseResponse<List<String>> getPermissionList(@RequestParam("spaceId") Long spaceId,
                                                          @RequestParam("userId") Long userId) {
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            return ResultUtils.success(List.of());
        }
        // 空间所有者拥有全部权限
        if (space.getUserId().equals(userId)) {
            return ResultUtils.success(spaceUserAuthManager.getPermissionsByRole("admin"));
        }
        // 查询用户是否为空间成员
        SpaceUser spaceUser = spaceUserService.getOne(
                new LambdaQueryWrapper<SpaceUser>()
                        .eq(SpaceUser::getSpaceId, spaceId)
                        .eq(SpaceUser::getUserId, userId)
        );
        if (spaceUser == null) {
            return ResultUtils.success(List.of());
        }
        return ResultUtils.success(spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole()));
    }
}
