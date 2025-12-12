package com.dong.dongpicturebackend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * @author by hongdou
 * @date 2025/10/12.
 * @DESC: 公共图片分析请求封装类
 * 各个具体的分析请求类继承该类
 */

@Data
public class SpaceAnalyzeRequest implements Serializable {

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 是否查询公共图库
     */
    private boolean queryPublic;

    private boolean queryAll;

    public static final long serialVersionUID = 1L;

}
