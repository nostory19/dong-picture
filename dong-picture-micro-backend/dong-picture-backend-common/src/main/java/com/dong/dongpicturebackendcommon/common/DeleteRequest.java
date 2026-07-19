package com.dong.dongpicturebackendcommon.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @author by hongdou
 * @date 2025/2/20.
 * @DESC: 用于通用的删除请求类
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionID = 1L;
}
