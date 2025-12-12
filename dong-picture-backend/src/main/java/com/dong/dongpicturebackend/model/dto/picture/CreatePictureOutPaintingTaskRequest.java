package com.dong.dongpicturebackend.model.dto.picture;

import com.dong.dongpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * @author by hongdou
 * @date 2025/10/10.
 * @DESC:
 */

@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {
    /**
     * 图片id
     * 根据图片id查询拿到url地址
     */
    private Long pictureId;

    /**
     * 扩图参数
     *
     */
    private CreateOutPaintingTaskRequest.Parameters parameters;

    private static final long serialVersionUID = 1L;
}
