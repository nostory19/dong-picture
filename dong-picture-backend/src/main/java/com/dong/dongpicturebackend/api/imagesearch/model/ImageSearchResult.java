package com.dong.dongpicturebackend.api.imagesearch.model;

import lombok.Data;

/**
 * @author by hongdou
 * @date 2025/9/6.
 * @DESC: 存储返回搜索结果的信息
 */
@Data
public class ImageSearchResult {
    /**
     * 缩略图地址
     */
    private String thumbUrl;

    /**
     * 来源地址
     */
    private String fromUrl;

}
