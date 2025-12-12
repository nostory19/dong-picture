package com.dong.dongpicturebackend.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.dong.dongpicturebackend.common.BaseResponse;
import com.dong.dongpicturebackend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author by hongdou
 * @date 2025/2/20.
 * @DESC: 全局异常处理器
 */

// 使用到环绕切面，可以在类中实现切面或者写切点
@RestControllerAdvice
// 日志注解
@Slf4j
public class GlobalExceptionHandler {
    // 指定捕获业务异常

    /**
     * 项目中只要抛出了BusinessException，都会被环绕切面所捕获，然后按照下面的方法进行处理
     * @return
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e){
        log.error("BusinessException", e);
        // 返回我们自己写的封装格式
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * 补充运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> businessExceptionHandler(RuntimeException e){
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    /**
     * 将Sa-Token的异常也转换为我们的异常格式
     */
    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> notLoginExceptionHandler(NotLoginException e){
        log.error("NotLoginException", e);
        return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, e.getMessage());
    }

    @ExceptionHandler(NotPermissionException.class)
    public BaseResponse<?> notPermissionExceptionHandler(NotPermissionException e){
        log.error("NotPermissionException", e);
        return ResultUtils.error(ErrorCode.NOT_AUTH_ERROR, e.getMessage());
    }
}
