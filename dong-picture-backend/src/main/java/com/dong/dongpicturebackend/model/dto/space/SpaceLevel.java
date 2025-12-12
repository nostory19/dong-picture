package com.dong.dongpicturebackend.model.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author by hongdou
 * @date 2025/8/22.
 * @DESC:
 */
@Data
@AllArgsConstructor
public class SpaceLevel {
    private int value;

    private String text;

    private long maxCount;

    private long maxSize;
}
