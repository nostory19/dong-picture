package com.dong.dongpicturebackend.model.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author by hongdou
 * @date 2025/10/14.
 * @DESC: 空间用户上传行为分析响应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceUserAnalyzeResponse implements Serializable {
    /**
     * 时间区间
     */
    private String period;

    /**
     * 用户上传图片数量
     */
    private Long count;



    public static final long serialVersionUID = 1L;
}
