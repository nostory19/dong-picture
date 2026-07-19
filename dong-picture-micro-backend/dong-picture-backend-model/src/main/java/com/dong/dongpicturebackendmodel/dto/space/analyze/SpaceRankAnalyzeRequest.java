package com.dong.dongpicturebackendmodel.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * @author by hongdou
 * @date 2025/10/14.
 * @DESC: 仅供管理员使用，空间排行分析请求
 * 直接返回前N名的空间，不用新的响应类了
 */
@Data
public class SpaceRankAnalyzeRequest implements Serializable {
    /**
     * 排名前N的空间
     */
    private Integer topN = 10;



    public static final long serialVersionUID = 1L;
}