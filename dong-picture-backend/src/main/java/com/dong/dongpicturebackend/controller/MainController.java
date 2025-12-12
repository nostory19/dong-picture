package com.dong.dongpicturebackend.controller;

import com.dong.dongpicturebackend.common.BaseResponse;
import com.dong.dongpicturebackend.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author by hongdou
 * @date 2025/2/20.
 * @DESC:
 */
@RestController
@RequestMapping("/") // 接口路径是一个根路径
public class MainController {

    /**
     * 健康检查，返回一个特别简单的值即可，查看项目是否正常运行。
     * @return
     */
    @GetMapping("/health")
    public BaseResponse<String> health(){
        return ResultUtils.success("ok");
    }
}
