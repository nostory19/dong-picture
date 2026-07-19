package com.dong.dongpicturebackendpictureservice.interfaces.controller;

import com.dong.dongpicturebackendcommon.common.BaseResponse;
import com.dong.dongpicturebackendcommon.common.ResultUtils;
import com.dong.dongpicturebackendcommon.exception.BusinessException;
import com.dong.dongpicturebackendcommon.exception.ErrorCode;
import com.dong.dongpicturebackendmodel.dto.picture.DoThumbRequest;
import com.dong.dongpicturebackendmodel.entity.User;
import com.dong.dongpicturebackendpictureservice.domain.picture.service.ThumbDomainService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/thumb")
public class ThumbController {

    @Resource
    private ThumbDomainService thumbDomainService;

    private User getLoginUser(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null || "null".equals(userIdStr) || userIdStr.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        User user = new User();
        user.setId(Long.valueOf(userIdStr));
        return user;
    }

    @PostMapping("/do")
    public BaseResponse<Boolean> doThumb(@RequestBody DoThumbRequest doThumbRequest,
                                          HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        thumbDomainService.doThumb(doThumbRequest.getPictureId(), loginUser);
        return ResultUtils.success(true);
    }

    @PostMapping("/undo")
    public BaseResponse<Boolean> undoThumb(@RequestBody DoThumbRequest doThumbRequest,
                                            HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        thumbDomainService.undoThumb(doThumbRequest.getPictureId(), loginUser);
        return ResultUtils.success(true);
    }
}
