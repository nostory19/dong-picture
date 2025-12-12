package com.dong.dongpicturebackend.common;

import lombok.Data;

/**
 * @author by hongdou
 * @date 2025/2/20.
 * @DESC: 分页请求类，以后查询用到了分页，就可以继承该类
 */
@Data
public class PageRequest {
    /**
     * 当前页号
     */
    private int current = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序
     */
    private String sortOrder = "descend"; // 默认升序
}
