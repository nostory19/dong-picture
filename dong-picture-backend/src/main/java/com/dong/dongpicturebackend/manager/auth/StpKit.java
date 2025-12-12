package com.dong.dongpicturebackend.manager.auth;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

/**
 * @author by hongdou
 * @date 2025/10/24.
 * @DESC: StpLogic门面类
 * 管理项目中所有的StpLogic账号体系
 * 添加@Component注解确保静态属性被初始化
 */
@Component
public class StpKit {
    public static final String SPACE_TYPE = "space";

    /**
     * 默认原生会话对象，项目中目前没有用到
     */
    public static final StpLogic DEFAULT = StpUtil.stpLogic;

    /**
     * Space会话对象，管理Space表所有账号的登录、权限认证
     * 之后就可以针对空间中的账号体系进行鉴权之类的操作了，与登录的账号体系区分开
     */
    public static final StpLogic SPACE = new StpLogic(SPACE_TYPE);
}
