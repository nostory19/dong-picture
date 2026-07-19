package com.dong.dongpicturebackendserviceclient.application.service;

import com.dong.dongpicturebackendcommon.common.BaseResponse;
import com.dong.dongpicturebackendmodel.entity.Space;
import com.dong.dongpicturebackendmodel.vo.SpaceVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "dong-picture-backend-space-service", path = "/inner")
public interface SpaceFeignClient {

    @GetMapping("/getById")
    BaseResponse<Space> getSpaceById(@RequestParam("spaceId") Long spaceId);

    @GetMapping("/getVOById")
    BaseResponse<SpaceVO> getSpaceVOById(@RequestParam("spaceId") Long spaceId);

    @PostMapping("/checkPermission")
    BaseResponse<Boolean> checkPermission(@RequestParam("spaceId") Long spaceId,
                                          @RequestParam("userId") Long userId,
                                          @RequestParam("permission") String permission);

    @PostMapping("/increaseUsage")
    BaseResponse<Boolean> increaseUsage(@RequestParam("spaceId") Long spaceId,
                                        @RequestParam("size") Long size,
                                        @RequestParam("count") Integer count);

    @PostMapping("/getPermissionList")
    BaseResponse<List<String>> getPermissionList(@RequestParam("spaceId") Long spaceId,
                                                  @RequestParam("userId") Long userId);
}
