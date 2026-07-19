package com.dong.dongpicturebackendmodel.dto.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.util.Date;

/**
 * @author by hongdou
 * @date 2025/6/20.
 * @DESC: 点击上传文件后，得到的图片信息的类
 */
@Data
public class UploadPictureResult {

    /**
     * 图片url
     */
    private String url;

    /**
     * 缩略图Url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 图片主色调
     * 上传图片的时候也要包含图片的主色调
     */
    private String picColor;


}