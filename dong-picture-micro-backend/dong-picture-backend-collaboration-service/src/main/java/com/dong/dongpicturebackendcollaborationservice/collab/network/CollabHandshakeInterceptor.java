package com.dong.dongpicturebackendcollaborationservice.collab.network;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 协作 WebSocket 握手拦截器 — 从 HTTP 请求提取 pictureId 和用户信息。
 */
@Slf4j
public class CollabHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest httpRequest = ((ServletServerHttpRequest) request).getServletRequest();

            String pictureId = httpRequest.getParameter("pictureId");
            if (StrUtil.isBlank(pictureId)) {
                log.warn("WebSocket handshake rejected: missing pictureId");
                return false;
            }

            String clientId = httpRequest.getParameter("clientId");
            String userId = httpRequest.getParameter("userId");
            String userName = httpRequest.getParameter("userName");

            attributes.put("pictureId", Long.valueOf(pictureId));
            if (clientId != null) attributes.put("clientId", clientId);
            if (userId != null) attributes.put("userId", Long.valueOf(userId));
            if (userName != null) attributes.put("userName", userName);

            log.info("WebSocket handshake OK: pictureId={}, clientId={}", pictureId, clientId);
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
    }
}
