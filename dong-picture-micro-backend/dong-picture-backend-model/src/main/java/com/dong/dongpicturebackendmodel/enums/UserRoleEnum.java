package com.dong.dongpicturebackendmodel.enums;

import cn.hutool.core.util.ObjUtil;
import com.dong.dongpicturebackendmodel.entity.User;
import lombok.Getter;

/**
 * @author by hongdou
 * @date 2025/2/24.
 * @DESC: 用户角色枚举
 */

@Getter
public enum UserRoleEnum {


    // 枚举对象
    USER("用户", "user"),
    ADMIN("管理员", "admin");

    // 枚举属性
    private final String text;

    private final String value;

    UserRoleEnum(String text, String value){
        this.text = text;
        this.value = value;
    }

    /**
     * 通过给定value获取到对象
     * @param value
     * @return
     */
    public static UserRoleEnum getEnumByValue(String value){
        // 检查是否为空
        if (ObjUtil.isEmpty(value)){
            return null;
        }
        // 不为空则找到value的对象
        // 当枚举值比较多的时候，如何进行优化，即将for循环利用map查询进行优化
        for (UserRoleEnum userRoleEnum : UserRoleEnum.values()){
            if (userRoleEnum.getValue().equals(value)){
                return userRoleEnum;
            }
        }

        return null;
    }
}