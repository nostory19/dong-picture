package com.dong.dongpicturebackend.model.dto.space;

import com.dong.dongpicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author by hongdou
 * @date 2025/8/19.
 * @DESC: 空间查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceQueryRequest extends PageRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 创建用户的id
     */
    private Long userId;

    /**
     * 空间级别
     */
    private Integer spaceLevel;

    /**
     * 增加的
     * 空间类型 0-私有空间 1-团队空间
     */
    private Integer spaceType;

    private static final long serialVersionUID = 1L;
}
