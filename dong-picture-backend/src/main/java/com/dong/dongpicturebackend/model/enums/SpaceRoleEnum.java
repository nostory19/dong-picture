package com.dong.dongpicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author by hongdou
 * @date 2025/10/16.
 * @DESC: 空间角色枚举
 */
@Getter
public enum SpaceRoleEnum {
    VIEWER("浏览者", "viewer"),
    EDITOR("编辑者", "editor"),
    ADMIN("管理员", "admin");

    private final String text;

    private final String value;

    SpaceRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value获取枚举
     */
    public static SpaceRoleEnum getEnumByValue(String value){
        if (ObjUtil.isEmpty(value)){
            return null;
        }
        for (SpaceRoleEnum spaceRoleEnum : SpaceRoleEnum.values()) {
            if (spaceRoleEnum.getValue().equals(value)){
                return spaceRoleEnum;
            }
        }
        return null;
    }

    /**
     * 获取所有的text
     * @return
     */
    public static List<String> getAllTexts(){
        return Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getText)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有的value
     * @return
     */
    public static List<String> getAllValues() {
        return Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getValue)
                .collect(Collectors.toList());
    }
}
