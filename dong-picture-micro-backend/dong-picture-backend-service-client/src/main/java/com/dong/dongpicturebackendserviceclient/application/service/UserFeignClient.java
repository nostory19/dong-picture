package com.dong.dongpicturebackendserviceclient.application.service;

import com.dong.dongpicturebackendcommon.common.BaseResponse;
import com.dong.dongpicturebackendmodel.entity.User;
import com.dong.dongpicturebackendmodel.vo.UserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "dong-picture-backend-user-service", path = "/inner")
public interface UserFeignClient {

    @GetMapping("/getById")
    BaseResponse<User> getUserById(@RequestParam("userId") Long userId);

    @GetMapping("/getVOById")
    BaseResponse<UserVO> getUserVOById(@RequestParam("userId") Long userId);

    @GetMapping("/getLoginUser")
    BaseResponse<User> getLoginUser(@RequestParam("token") String token);

    @PostMapping("/listByIds")
    BaseResponse<List<User>> listUserByIds(@RequestBody List<Long> userIds);
}
