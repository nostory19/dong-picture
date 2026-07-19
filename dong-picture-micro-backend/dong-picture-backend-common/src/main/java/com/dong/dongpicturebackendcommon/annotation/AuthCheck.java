package com.dong.dongpicturebackendcommon.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author by hongdou
 * @date 2025/5/28.
 * @DESC:
 */
// 指定注解的生效范围
@Target(ElementType.METHOD)
// 指定注解在什么时候生效
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 表示必须具有哪一个角色
     * 这里规定，只要用了这个注解就要求必须登录了才能使用的权限
     * @return
     */
    String mustRole() default "";
}
