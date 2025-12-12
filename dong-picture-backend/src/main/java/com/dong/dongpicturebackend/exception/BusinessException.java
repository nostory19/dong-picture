package com.dong.dongpicturebackend.exception;

import lombok.Getter;

/**
 * @author by hongdou
 * @date 2025/2/15.
 * @DESC: 自定义业务异常
 */
@Getter
public class BusinessException extends RuntimeException{

    // 也可以像状态码一样定义一个异常码
    private final int code;

    public BusinessException(int code, String message) {
        super(message); // 继承父类
        this.code = code;
    }

    // 根据状态错误码
    public BusinessException(ErrorCode errorCode){
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message){
        super(message);
        this.code = errorCode.getCode();
    }
}
