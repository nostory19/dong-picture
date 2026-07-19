package com.dong.dongpicturebackenduserservice.domain.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dong.dongpicturebackendmodel.dto.user.UserQueryRequest;
import com.dong.dongpicturebackendmodel.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dong.dongpicturebackendmodel.vo.LoginUserVO;
import com.dong.dongpicturebackendmodel.vo.UserVO;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 25141
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-02-24 15:56:47
*/
public interface UserService extends IService<User> {

    /**
     *
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return 为什么是long类型的，因为用户注册返回的是注册后用户的id，因此是long类型的。
     */

    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 获取加密后的密码
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     *
     * @param userAccount
     * @param userPassword
     * @param request
     * @return 登录成功后返回脱敏后的数据
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 请求获得脱敏后的用户信息
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 请求获得脱敏后的用户信息-适用于普通用户请求返回数据
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 请求获得脱敏后的用户信息 列表 -适用于普通用户请求返回数据
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取当前登录用户，用于业务逻辑之间使用
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销 （退出登陆）
     * @param request
     * @return
     */
    boolean userLogOut(HttpServletRequest request);

    /**
     * 将普通的java对象转换成mybatis需要的querymapper
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> gerQueryMapper(UserQueryRequest userQueryRequest);

    /**
     * 判断用户是否是管理员
     * @param user
     * @return
     */
    boolean isAdmin(User user);
}
