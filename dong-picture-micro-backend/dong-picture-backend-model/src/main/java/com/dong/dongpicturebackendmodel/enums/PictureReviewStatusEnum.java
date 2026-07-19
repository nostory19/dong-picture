package com.dong.dongpicturebackendmodel.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * @author by hongdou
 * @date 2025/2/24.
 * @DESC: 图片审核状态枚举
 */

@Getter
public enum PictureReviewStatusEnum {


    // 枚举对象
    REVIEWING("待审核", 0),
    PASS("通过", 1),
    REJECT("拒绝", 2);


    // 枚举属性
    private final String text;

    private final int value;

    PictureReviewStatusEnum(String text, int value){
        this.text = text;
        this.value = value;
    }

    /**
     * 通过给定value获取到对象
     * @param value
     * @return
     */
    public static PictureReviewStatusEnum getEnumByValue(Integer value){
        // 检查是否为空
        if (ObjUtil.isEmpty(value)){
            return null;
        }
        // 不为空则找到value的对象
        // 当枚举值比较多的时候，如何进行优化，即将for循环利用map查询进行优化
        for (PictureReviewStatusEnum pictureReviewStatusEnum : PictureReviewStatusEnum.values()){
            if (pictureReviewStatusEnum.value == value){
                return pictureReviewStatusEnum;
            }
        }

        return null;
    }
}