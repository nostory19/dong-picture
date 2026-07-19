package com.dong.dongpicturebackendspaceservice.interfaces.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.dong.dongpicturebackendcommon.common.BaseResponse;
import com.dong.dongpicturebackendcommon.common.DeleteRequest;
import com.dong.dongpicturebackendcommon.common.ResultUtils;
import com.dong.dongpicturebackendcommon.exception.BusinessException;
import com.dong.dongpicturebackendcommon.exception.ErrorCode;
import com.dong.dongpicturebackendcommon.exception.ThrowUtils;
import com.dong.dongpicturebackendmodel.dto.spaceuser.SpaceUserAddRequest;
import com.dong.dongpicturebackendmodel.dto.spaceuser.SpaceUserEditRequest;
import com.dong.dongpicturebackendmodel.dto.spaceuser.SpaceUserQueryRequest;
import com.dong.dongpicturebackendmodel.entity.SpaceUser;
import com.dong.dongpicturebackendmodel.entity.User;
import com.dong.dongpicturebackendmodel.vo.SpaceUserVO;
import com.dong.dongpicturebackendserviceclient.application.service.UserFeignClient;
import com.dong.dongpicturebackendspaceservice.domain.space.service.SpaceUserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author by hongdou
 * @date 2025/10/16.
 * @DESC: 空间用户关联控制器
 * 先实现基础功能
 * 然后加上权限管理
 */
@RestController
@RequestMapping("/spaceUser")
@Slf4j
public class SpaceUserController {
    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserFeignClient userFeignClient;

    /**
     * 添加成员到空间
     * @param spaceUserAddRequest
     * @return
     */
    @PostMapping("/add")
    // TODO: 原使用Sa-Token的@SaSpaceCheckPermission注解进行权限校验，现已移除
    // 原注解: @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    // 需要在网关层或拦截器层实现对应的空间权限校验逻辑
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest){
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 仅空间管理员可添加成员
        long id = spaceUserService.addSpaceUser(spaceUserAddRequest);
        return ResultUtils.success(id);

    }

    /**
     * 从空间删除成员
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    // TODO: 原使用Sa-Token的@SaSpaceCheckPermission注解进行权限校验，现已移除
    // 原注解: @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    // 需要在网关层或拦截器层实现对应的空间权限校验逻辑
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
        if (deleteRequest == null || deleteRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 执行删除
        boolean result = spaceUserService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询某个成员在某个空间的信息
     * @param spaceUserQueryRequest
     * @return
     */
    @PostMapping("/get")
    // TODO: 原使用Sa-Token的@SaSpaceCheckPermission注解进行权限校验，现已移除
    // 原注解: @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    // 需要在网关层或拦截器层实现对应的空间权限校验逻辑
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest){
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 关联查询用户信息和空间信息
        Long userId = spaceUserQueryRequest.getUserId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
        // 查询数据库
        SpaceUser spaceUser = spaceUserService.getOne(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询成员信息列表
     * @param spaceUserQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list")
    // TODO: 原使用Sa-Token的@SaSpaceCheckPermission注解进行权限校验，现已移除
    // 原注解: @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    // 需要在网关层或拦截器层实现对应的空间权限校验逻辑
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest, HttpServletRequest request){
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 查询
        List<SpaceUser> spaceUserList = spaceUserService.list(
                spaceUserService.getQueryWrapper(spaceUserQueryRequest)
        );
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }

    /**
     * 编辑成员信息
     * 设置权限
     * @param spaceUserEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    // TODO: 原使用Sa-Token的@SaSpaceCheckPermission注解进行权限校验，现已移除
    // 原注解: @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    // 需要在网关层或拦截器层实现对应的空间权限校验逻辑
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest, HttpServletRequest request){
        if (spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将dtO转换为entity
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserEditRequest, spaceUser);
        // 判断是否存在
        Long id = spaceUserEditRequest.getId();
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = spaceUserService.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request){
        // 获取当前登录用户
        Long userId = getLoginUser(request).getId();
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(userId);
        List<SpaceUser> spaceUserList = spaceUserService.list(
                spaceUserService.getQueryWrapper(spaceUserQueryRequest)
        );
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }

    /**
     * 获取当前登录用户
     * TODO: 微服务中通过网关传递的header获取用户信息，替代原来monolith中的session方式
     * 原实现: userService.getLoginUser(request) 从HttpSession中获取
     * 新实现: 从request header "X-User-Id"获取userId，通过Feign调用user-service获取用户信息
     */
    private User getLoginUser(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-User-Id");
        if (StrUtil.isBlank(userIdStr)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        Long userId = Long.valueOf(userIdStr);
        User user = userFeignClient.getUserById(userId).getData();
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户不存在");
        }
        return user;
    }
}
