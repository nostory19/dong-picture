package com.dong.dongpicturebackendmodel.enums;

import lombok.Getter;

@Getter
public enum ThumbTypeEnum {
    INCR(1),
    DECR(-1),
    NON(0);

    private final int value;

    ThumbTypeEnum(int value) {
        this.value = value;
    }
}
