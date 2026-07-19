package com.dong.dongpicturebackendmodel.vo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 已登录用户视图脱敏
 */
@Data
public class LoginUserVO implements Serializable {
    /**
     * id
     */
//    让其自动生成一个较长整形的id，避免生成1、2、3、4这样的数据避免被爬
//    @TableId(type = IdType.ASSIGN_ID)
//    为了方便我还是使用auto，方便内部使用管理
    private Long id;

    /**
     * 账号
     */
    private String userAccount;


    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * JWT token（登录成功后返回）
     */
    private String token;


    private static final long serialVersionUID = 1L;
}