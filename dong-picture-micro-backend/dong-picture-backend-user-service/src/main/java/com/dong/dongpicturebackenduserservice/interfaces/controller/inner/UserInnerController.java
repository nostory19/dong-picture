package com.dong.dongpicturebackenduserservice.interfaces.controller.inner;

import com.dong.dongpicturebackendcommon.common.BaseResponse;
import com.dong.dongpicturebackendcommon.common.ResultUtils;
import com.dong.dongpicturebackendcommon.exception.BusinessException;
import com.dong.dongpicturebackendcommon.exception.ErrorCode;
import com.dong.dongpicturebackendcommon.exception.ThrowUtils;
import com.dong.dongpicturebackendmodel.entity.User;
import com.dong.dongpicturebackendmodel.vo.UserVO;
import com.dong.dongpicturebackendserviceclient.application.service.UserFeignClient;
import com.dong.dongpicturebackenduserservice.domain.user.service.UserService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author by hongdou
 * @date 2025/7/12.
 * @DESC: 用户内部接口，实现UserFeignClient
 */
@RestController
@RequestMapping("/inner")
public class UserInnerController implements UserFeignClient {

    @Resource
    private UserService userService;

    @Override
    @GetMapping("/getById")
    public BaseResponse<User> getUserById(@RequestParam("userId") Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    @Override
    @GetMapping("/getVOById")
    public BaseResponse<UserVO> getUserVOById(@RequestParam("userId") Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        UserVO userVO = userService.getUserVO(user);
        return ResultUtils.success(userVO);
    }

    @Override
    @GetMapping("/getLoginUser")
    public BaseResponse<User> getLoginUser(@RequestParam("token") String token) {
        // TODO: Implement JWT token parsing to extract user info
        ThrowUtils.throwIf(token == null || token.isEmpty(), ErrorCode.PARAMS_ERROR, "token不能为空");
        // TODO: Parse JWT token, extract user ID, then query user from database
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "JWT token parsing not yet implemented");
    }

    @Override
    @PostMapping("/listByIds")
    public BaseResponse<List<User>> listUserByIds(@RequestBody List<Long> userIds) {
        ThrowUtils.throwIf(userIds == null || userIds.isEmpty(), ErrorCode.PARAMS_ERROR);
        List<User> users = userService.listByIds(userIds);
        return ResultUtils.success(users);
    }
}
