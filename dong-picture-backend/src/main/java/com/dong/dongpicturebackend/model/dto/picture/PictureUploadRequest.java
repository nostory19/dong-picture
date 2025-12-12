package com.dong.dongpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @author by hongdou
 * @date 2025/6/20.
 * @DESC: 图片上传请求的dto
 * 对于管理员：有创建，编辑，和查看列表等功能，不需要数据脱敏
 * 对于用户，有新建，查看，等功能，需要数据脱敏
 */
@Data
public class PictureUploadRequest implements Serializable {
    /**
     * 图片id（用于修改）
     */
    private Long id;

    /**
     * 文件地址
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 空间id
     */
    private Long spaceId;

    private static final long serialVersionID = 1L;
}
