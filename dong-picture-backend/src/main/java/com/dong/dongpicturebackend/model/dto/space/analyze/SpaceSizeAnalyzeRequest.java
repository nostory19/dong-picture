package com.dong.dongpicturebackend.model.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by hongdou
 * @date 2025/10/14.
 * @DESC: 空间图片大小分析请求
 */
// 确保在比较两个SpaceSizeAnalyzeRequest对象时，不仅比较当前类的字段
    // 还会比较其父类SpaceAnalyzeRequest的字段
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceSizeAnalyzeRequest extends SpaceAnalyzeRequest{
}
