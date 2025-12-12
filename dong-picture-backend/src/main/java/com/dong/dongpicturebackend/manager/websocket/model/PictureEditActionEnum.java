package com.dong.dongpicturebackend.manager.websocket.model;

import lombok.Getter;

/**
 * @author by hongdou
 * @date 2025/12/10.
 * @DESC: 图片编辑操作枚举
 */

@Getter
public enum PictureEditActionEnum {
    ZOOM_IN("放大操作", "ZOOM_IN"),
    ZOOM_OUT("缩小操作", "ZOOM_OUT"),
    ROTATE_LEFT("左旋操作", "ROTATE_LEFT"),
    ROTATE_RIGHT("右旋操作", "ROTATE_RIGHT");

    private final String text;
    private final String value;

    PictureEditActionEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value获取枚举
     *
     */
    public static PictureEditActionEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (PictureEditActionEnum actionEnum : values()) {
            if (actionEnum.getValue().equals(value)) {
                return actionEnum;
            }
        }
        return null;
    }
}
