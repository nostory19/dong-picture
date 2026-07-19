package com.dong.dongpicturebackendmodel.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * @author by hongdou
 * @date 2025/8/19.
 * @DESC: 空间级别枚举类
 */
@Getter
public enum SpaceLevelEnum {

    // 创建枚举选项
    COMMON("普通版", 0, 100, 100L * 1024 * 1024),
    PROFESSIONAL("专业版", 1, 1000, 1000L * 1024 * 1024),
    FLAGSHIP("旗舰版", 2, 10000, 10000L * 1024 * 1024);

    // 先定义定义属性
    private final String text;

    private final int value;

    private final long maxCount;

    private final long maxSize;


    // 定义方法

    /**
     *
     * @param text 文本
     * @param value 值
     * @param maxCount 最大图片总大小
     * @param maxSize 最大图片总数量
     */
    SpaceLevelEnum(String text, int value, int maxCount, long maxSize) {
        this.text = text;
        this.value = value;
        this.maxCount = maxCount;
        this.maxSize = maxSize;
    }

    /**
     * 根据value获取枚举
     * @param value
     * @return
     */
    public static SpaceLevelEnum getEnumByValue(Integer value){
        if (ObjUtil.isEmpty(value)){
            return null;
        }
        for (SpaceLevelEnum spaceLevelEnum : SpaceLevelEnum.values()){
            if (spaceLevelEnum.value == value){
                return spaceLevelEnum;
            }
        }
        return null;
    }



}