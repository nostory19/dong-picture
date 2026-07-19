package com.dong.dongpicturebackendmodel.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @author by hongdou
 * @date 2025/9/23.
 * @DESC:
 */

@Data
public class SearchPictureByColorRequest implements Serializable {
    /**
     * 颜色主色调
     */
    private String picColor;

    private Long spaceId;

    private static final long serialVersionUID = 1L;
}