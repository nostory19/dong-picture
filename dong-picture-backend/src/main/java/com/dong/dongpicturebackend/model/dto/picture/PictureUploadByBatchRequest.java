package com.dong.dongpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @author by hongdou
 * @date 2025/7/6.
 * @DESC: 批量导入图片
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {
    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 抓取数量
     */
    private Integer count = 10;

    /**
     * 名称前缀
     * 让管理员指定批量导入图片的前缀，便于区分
     */
    private String namePrefix;

    private static final long serialVersionUID = 1L;

}
