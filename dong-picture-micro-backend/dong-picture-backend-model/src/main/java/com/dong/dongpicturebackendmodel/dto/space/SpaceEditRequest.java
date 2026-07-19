package com.dong.dongpicturebackendmodel.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * @author by hongdou
 * @date 2025/8/19.
 * @DESC: 空间编辑请求，仅给用户使用，仅可编辑空间名称
 */
@Data
public class SpaceEditRequest implements Serializable {

    /**
     * 空间id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String name;

    private static final long serialVersionUID = 1L;

}