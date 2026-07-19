package com.dong.dongpicturebackendcommon.common;

import com.dong.dongpicturebackendcommon.exception.ErrorCode;

/**
 * @author by hongdou
 * @date 2025/2/15.
 * @DESC: 响应结果工具类
 */
public class ResultUtils {

    /**
     * 成功
     * @param data 数据
     * @return 响应
     * @param <T> 数据类型
     */
    public static <T> BaseResponse<T> success(T data){
        return new BaseResponse<>(0, data, "ok");
    }


    /**
     * 失败
     * @param errorCode
     * @return
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode){
        return new BaseResponse<>(errorCode);
    }

    public static BaseResponse<?> error(int code, String message){
        return new BaseResponse<>(code, null, message);
    }

    public static BaseResponse<?> error(ErrorCode errorCode, String message){
        // 自己指定错误信息
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }




}
