package com.dong.dongpicturebackendgateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

/**
 * WebSocket 路由 — 使用 Java DSL 注册，不受 Nacos YAML 配置覆盖。
 *
 * 路由顺序由 @Order(0) 控制，确保在通用 /api/picture/** 之前匹配。
 */
@Configuration
public class WebSocketRouteConfig {

    /**
     * 直连 picture-service 的 WebSocket 端点。
     * 不使用 Nacos 服务发现（lb:ws://），避免本地开发时服务发现失败。
     * 生产环境可改为 lb:ws://dong-picture-backend-picture-service。
     */
    @Bean
    public RouteLocator pictureWebSocketRoute(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("websocket-picture-edit", r -> r
                        .path("/api/picture/ws/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri(URI.create("ws://localhost:8203"))
                )
                .build();
    }
}
