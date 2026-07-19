package com.dong.dongpicturebackenduserservice.interfaces.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dong.dongpicturebackendcommon.annotation.AuthCheck;
import com.dong.dongpicturebackendcommon.common.BaseResponse;
import com.dong.dongpicturebackendcommon.common.DeleteRequest;
import com.dong.dongpicturebackendcommon.common.ResultUtils;
import com.dong.dongpicturebackendcommon.constant.UserConstant;
import com.dong.dongpicturebackendcommon.exception.BusinessException;
import com.dong.dongpicturebackendcommon.exception.ErrorCode;
import com.dong.dongpicturebackendcommon.exception.ThrowUtils;
import com.dong.dongpicturebackendmodel.dto.user.*;
import com.dong.dongpicturebackendmodel.entity.User;
import com.dong.dongpicturebackendmodel.enums.UserRoleEnum;
import com.dong.dongpicturebackendmodel.vo.LoginUserVO;
import com.dong.dongpicturebackendmodel.vo.UserVO;
import com.dong.dongpicturebackenduserservice.domain.user.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author by hongdou
 * @date 2025/2/20.
 * @DESC:
 */
@RestController
// 使用restcontroller自动把返回格式转换成json格式
@RequestMapping("/user") // 接口路径是一个根路径
public class UserController {
    @Resource
    private UserService userService;


    /**
     * 用户注册接口
     * 接收的是一个对象的格式
     *
     * @return 下面有一个示例，权限拦截器如何使用，我们设置的是添加了权限注解的就会拦截并判断，因此可以添加一个authCheck的注解
     * 这样注册接口就会校验是否有权限才能使用该接口
     */
    //    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegistRequest userRegistRequest) {
        // 返回的是用户的ID
        // 先判断对象是否满足条件，不满足直接返回
        ThrowUtils.throwIf(userRegistRequest == null, ErrorCode.PARAMS_ERROR);

        String userAccount = userRegistRequest.getUserAccount();
        String userPassword = userRegistRequest.getUserPassword();
        String checkPassword = userRegistRequest.getCheckPassword();

        long userId = userService.userRegister(userAccount, userPassword, checkPassword);

        return ResultUtils.success(userId);
    }

    /**
     * @param userLoginRequest
     * @param request
     * @return 返回登录态后脱敏后的信息
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 先判断是否为空
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取对象
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();

        // 实现service
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        // 返回
        return ResultUtils.success(loginUserVO);
    }

    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null || "null".equals(userIdStr) || userIdStr.isEmpty()) {
            return new BaseResponse<>(ErrorCode.NOT_LOGIN_ERROR.getCode(), null, "未登录");
        }
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }


    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogOut(HttpServletRequest request) {
        boolean logOut = userService.userLogOut(request);
        return ResultUtils.success(logOut);
    }

    /**
     * 管理员新增用户，返回用户的id
     *
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
    // TODO: Replace @AuthCheck with JWT-based authorization interceptor
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        // 返回的是用户的ID
        // 先判断对象是否满足条件，不满足直接返回
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 然后将请求创建为user对象
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);  // 将原始对象转化为目标对象
        // 用户新增需要设置默认密码
        // 密码也需要加密
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);

        // 然后就可以写入数据库
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 获取用户，管理员获取
     * @param id
     * @return
     */
    @GetMapping("/get")
    // TODO: Replace @AuthCheck with JWT-based authorization interceptor
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(Long id) {
        // 判断id是否满足条件   \
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 直接调用service的就好了
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 获取用户脱敏信息
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> gerUserVO(long id){
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 先查询用户，然后封装脱敏信息
        BaseResponse<User> userById = getUserById(id);  //复用controller的代码
        User user = userById.getData();
        return ResultUtils.success(userService.getUserVO(user));

    }

    /**
     * 删除用户
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    // TODO: Replace @AuthCheck with JWT-based authorization interceptor
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest){
        // 根据id进行删除
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }


    /**
     * 更新操作 和add类似
     * @param userUpdateRequest
     * @return
     */
    @PostMapping("/update")
    // TODO: Replace @AuthCheck with JWT-based authorization interceptor
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        // 返回的是用户的ID
        // 先判断对象是否满足条件，不满足直接返回
        ThrowUtils.throwIf(userUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        // 然后将请求创建为user对象
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);  // 将原始对象转化为目标对象
        // 直接更新
        // 然后就可以写入数据库
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 用户分页查询
     * @param userQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    // TODO: Replace @AuthCheck with JWT-based authorization interceptor
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage (@RequestBody UserQueryRequest userQueryRequest){
        // 先判断是否为空
        ThrowUtils.throwIf(userQueryRequest ==null, ErrorCode.PARAMS_ERROR);
        // 获取当前页和页面大小
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        // 创建Page
        Page<User> userPage = userService.page(new Page<>(current, pageSize), userService.gerQueryMapper(userQueryRequest));
        // 将得到的userPage脱敏
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        // 获取用户脱敏的list
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        // 将脱敏list放到page中
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);

    }
}
