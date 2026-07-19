package com.dong.dongpicturebackendmodel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("thumb")
public class Thumb implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long pictureId;
    private Date createTime;
    private static final long serialVersionUID = 1L;
}
