package com.dong.dongpicturebackendmodel.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by hongdou
 * @date 2025/10/14.
 * @DESC: 空间用户上传行为分析
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceUserAnalyzeRequest extends SpaceAnalyzeRequest{
    // 增加用户id和时间维度，可以分析用户在某个时间段内的上传行为

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 时间维度
     */
    private String timeDimension;

}