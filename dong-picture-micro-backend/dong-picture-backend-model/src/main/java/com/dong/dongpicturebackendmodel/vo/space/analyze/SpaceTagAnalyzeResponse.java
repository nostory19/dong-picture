package com.dong.dongpicturebackendmodel.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author by hongdou
 * @date 2025/10/14.
 * @DESC: 图片标签分析响应封装类
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SpaceTagAnalyzeResponse implements Serializable {
    // 返回标签名称和对应次数
    /**
     * 标签名称
     */
    private String tag;

    /**
     * 使用次数
     */
    private Long count;


    public static final long serialVersionUID = 1L;
}