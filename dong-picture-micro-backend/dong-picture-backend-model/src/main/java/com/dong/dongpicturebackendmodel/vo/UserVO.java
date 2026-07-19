package com.dong.dongpicturebackendmodel.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户视图脱敏，和登录用户返回用户视图类似，用户的请求获得用户信息后也要脱敏返回
 */
@Data
public class UserVO implements Serializable {
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
     * 创建时间
     */
    private Date createTime;



    private static final long serialVersionUID = 1L;
}