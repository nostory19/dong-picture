package com.dong.dongpicturebackendmodel.dto.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.dong.dongpicturebackendcommon.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author by hongdou
 * @date 2025/5/30.
 * @DESC: 用户查询请求
 */

/**
 * 既然我们写了一个公用分页，则以后的查询请求都可以继承分类类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {
    /**
     * id
     */
    private Long id;

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
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;


    private static final long serialVersionUID = 1L;
}