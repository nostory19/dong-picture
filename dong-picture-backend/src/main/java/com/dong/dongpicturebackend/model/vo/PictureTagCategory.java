package com.dong.dongpicturebackend.model.vo;

import lombok.Data;

import java.util.List;

/**
 * @author by hongdou
 * @date 2025/6/22.
 * @DESC:
 */
@Data
public class PictureTagCategory {

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 分类列表
     */
    private List<String> categoryList;
}
