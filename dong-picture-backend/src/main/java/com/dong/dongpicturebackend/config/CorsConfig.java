package com.dong.dongpicturebackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author by hongdou
 * @date 2025/2/20.
 * @DESC:
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings (CorsRegistry registry){
        // 覆盖所有请求
        registry.addMapping("/**")
                // 允许发送Cookie
                .allowCredentials(true)
                // 放行哪些域名(使用Patterns)
                .allowedOriginPatterns("*")
                // 支持哪些方法跨域
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许的请求头
                .allowedHeaders("*")
                // 跨域时暴露请求头
                .exposedHeaders("*");


    }
}
