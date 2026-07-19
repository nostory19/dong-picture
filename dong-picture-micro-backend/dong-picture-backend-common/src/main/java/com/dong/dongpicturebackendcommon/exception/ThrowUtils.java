package com.dong.dongpicturebackendcommon.exception;

/**
 * @author by hongdou
 * @date 2025/2/15.
 * @DESC: 异常处理工具类
 */
public class ThrowUtils {
    /**
     * 最简的一种类似断言类的写法
     * @param condition
     * @param runtimeException
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException){
        if (condition){
            // 抛出异常
            throw runtimeException;
        }
    }


    // 函数重载，利用我们写的ErrorCode

    /**
     *
     * @param condition
     * @param errorCode
     */
    public static void throwIf(boolean condition, ErrorCode errorCode){
//       if (condition){
//           throw new BusinessException(errorCode);
//       }
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * 一层层封装，加上错误消息
     * @param condition
     * @param errorCode
     * @param message 错误消息
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message){
        throwIf(condition, new BusinessException(errorCode, message));
    }
}
