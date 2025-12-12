package com.dong.dongpicturebackend.model.vo.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * @author by hongdou
 * @date 2025/10/12.
 * @DESC: 空间占用分析响应封装类
 */
@Data
public class SpaceUsageAnalyzeResponse implements Serializable {
    // 返回空间占用情况涉及的字段

    /**
     * 已使用大小
     */
    private Long usedSize;

    /**
     * 总大小
     */
    private Long maxSize;

    /**
     * 空间使用比例
     */
    private Double sizeUsageRatio;

    /**
     * 当前图片数量
     */
    private Long usedCount;

    /**
     * 最大图片数量
     */
    private Long maxCount;

    /**
     * 图片数量使用比例
     */
    private Double countUsageRatio;


    public static final long serialVersionUID = 1L;
}
