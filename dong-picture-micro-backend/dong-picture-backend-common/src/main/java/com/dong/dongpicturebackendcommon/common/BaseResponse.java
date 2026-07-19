package com.dong.dongpicturebackendcommon.common;

import com.dong.dongpicturebackendcommon.exception.ErrorCode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author by hongdou
 * @date 2025/2/15.
 * @DESC: 全局响应封装类
 */

@Data
@NoArgsConstructor
public class BaseResponse<T> implements Serializable {
    // 定义返回的字段
    private int code;

    /**
     * 不确定返回数据的类型，使用泛型，在使用或者创建响应类对象时手动指定泛型
     */
    private T data;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    // 应对不同情况，多写几个构造函数
    public BaseResponse(int code, T data){
        // 调用之前的构造函数传递即可
        this(code, data, "");
    }

    // 假设传入的是错误状态码对象
    public BaseResponse(ErrorCode errorCode){
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
