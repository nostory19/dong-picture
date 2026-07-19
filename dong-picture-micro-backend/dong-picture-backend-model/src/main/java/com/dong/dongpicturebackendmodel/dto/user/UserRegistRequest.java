package com.dong.dongpicturebackendmodel.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author by hongdou
 * @date 2025/2/24.
 * @DESC: 用户注册请求的封装类
 * 一般请求封装类就是普通的get set方法
 */
@Data
public class UserRegistRequest implements Serializable {

    private static final long serialVersionUID = -1302297832045660713L;
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;

    // 实现序列化接口，还需要指定序列化ID

}