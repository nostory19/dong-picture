package com.dong.dongpicturebackendmodel.dto.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author by hongdou
 * @date 2025/5/30.
 * @DESC: 用户创建请求-管理员使用的接口，用户用户创建，区别用户注册
 */
@Data
public class UserAddRequest implements Serializable {
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
//    private String userPassword;
    // 后面实现的时候为创建用户自动设置一个默认密码
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


    private static final long serialVersionUID = 1L;
}