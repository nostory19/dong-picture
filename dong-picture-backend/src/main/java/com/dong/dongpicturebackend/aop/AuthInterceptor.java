package com.dong.dongpicturebackend.aop;

import com.dong.dongpicturebackend.annotation.AuthCheck;
import com.dong.dongpicturebackend.exception.BusinessException;
import com.dong.dongpicturebackend.exception.ErrorCode;
import com.dong.dongpicturebackend.model.entity.User;
import com.dong.dongpicturebackend.model.enums.UserRoleEnum;
import com.dong.dongpicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @author by hongdou
 * @date 2025/5/30.
 * @DESC:
 */
@Aspect
// 使用注解@Aspect表明是一个切面
@Component
// spring bean让spring识别加载
public class AuthInterceptor {

    // 执行权限校验，要用到用户服务
    @Resource
    private UserService userService;

    // 添加切点
    // 这里有前置、后置，环绕,Before, After, Around

    /**
     *
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     * @return
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {

        // 获取注解中的权限要求
        String mustRole = authCheck.mustRole();
        // 怎么拿到当前用户的登陆信息呢？
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        // 转换为servlet
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        User loginUser = userService.getLoginUser(request);

        // 获取用户的权限类型
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 如果权限要求为空，则继续执行原来的方法
        if (mustRoleEnum == null){
            return joinPoint.proceed();
        }
        // 反之，以下的代码就是必须有权限才会通过
        // 也要将获取到当前用户的角色转换成枚举类，方便使用
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 如果为空则异常
        if (userRoleEnum == null){
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }
        // 要对必须有管理员权限，即mustRoleEnum为ADMIN，但是用户的权限不是管理员权限，则异常
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)){
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }
        // 其他情况就是通过权限校验的
        return joinPoint.proceed();
    }

}
