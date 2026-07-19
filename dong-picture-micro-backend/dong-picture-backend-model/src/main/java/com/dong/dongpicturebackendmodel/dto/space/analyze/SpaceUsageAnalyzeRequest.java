package com.dong.dongpicturebackendmodel.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by hongdou
 * @date 2025/10/12.
 * @DESC: 空间占用分析请求封装类
 * 直接继承公共请求类，无需额外参数
 * 对应有一个空间占用分析响应类
 */

@EqualsAndHashCode(callSuper=false)
@Data
public class SpaceUsageAnalyzeRequest extends SpaceAnalyzeRequest{
}