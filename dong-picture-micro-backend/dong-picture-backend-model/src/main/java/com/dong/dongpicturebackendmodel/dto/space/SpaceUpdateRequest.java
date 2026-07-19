package com.dong.dongpicturebackendmodel.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * @author by hongdou
 * @date 2025/8/19.
 * @DESC: 空间更细请求，给管理员使用，可以修改空间级别和限额
 */
@Data
public class SpaceUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别
     */
    private Integer spaceLevel;

    /**
     * 空间的图片最大总大小
     */
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    private Long maxCount;

    private static final long serialVersionUID = 1L;
}