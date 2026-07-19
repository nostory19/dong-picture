package com.dong.dongpicturebackendpictureservice.websocket;

import cn.hutool.core.util.StrUtil;
import com.dong.dongpicturebackendmodel.entity.Picture;
import com.dong.dongpicturebackendmodel.entity.User;
import com.dong.dongpicturebackendpictureservice.domain.picture.service.PictureService;
import com.dong.dongpicturebackendserviceclient.application.service.UserFeignClient;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author by hongdou
 * @date 2025/12/10.
 * @DESC: websocket拦截器，连接前先校验
 */

@Component
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    private static final String JWT_SECRET = "dong-picture-jwt-secret-key-change-me";

    @Resource
    private PictureService pictureService;

    @Resource
    private UserFeignClient userFeignClient;

    /**
     * 握手前校验，校验用户是否有编辑图片的权限
     * @param request
     * @param response
     * @param wsHandler
     * @param attributes 给websocket会话添加属性
     * @return
     * @throws Exception
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // 获取当前用户
            // 获取HttpServletrequest
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest httpServletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            // 从请求中获取参数，例如?pictureId
            String pictureId = httpServletRequest.getParameter("pictureId");
            // 如果不存在
            if(StrUtil.isBlank(pictureId)){
                log.error("缺少图片参数，拒绝握手");
                return false;
            }
            // 从 URL token 参数解析 JWT 获取用户
            User loginUser = resolveUserFromToken(httpServletRequest);
            if (loginUser == null) {
                log.warn("WebSocket握手：未解析到用户，允许匿名连接");
            } else {
                log.info("WebSocket握手：用户 {} (id={})", loginUser.getUserName(), loginUser.getId());
            }
            // 校验图片是否存在
            Picture picture = pictureService.getById(pictureId);
            if (picture == null) {
                log.error("图片不存在，拒绝握手");
                return false;
            }
            // 设置用户信息到 websocket 会话
            attributes.put("user", loginUser);
            attributes.put("userId", loginUser != null ? loginUser.getId() : null);
            attributes.put("pictureId", Long.valueOf(pictureId));
        }


        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }

    /**
     * 从 URL token 参数解析 JWT，并通过 UserFeignClient 获取用户对象。
     */
    private User resolveUserFromToken(HttpServletRequest request) {
        String token = request.getParameter("token");
        if (StrUtil.isBlank(token)) {
            return null;
        }
        try {
            SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Object userIdObj = claims.get("userId");
            if (userIdObj == null) return null;
            Long userId = Long.valueOf(userIdObj.toString());
            // 通过 Feign 获取完整用户对象
            com.dong.dongpicturebackendcommon.common.BaseResponse<User> resp = userFeignClient.getUserById(userId);
            if (resp != null && resp.getData() != null) {
                return resp.getData();
            }
            // Feign 不可用时构造最小 User 对象
            User fallback = new User();
            fallback.setId(userId);
            fallback.setUserName(String.valueOf(claims.getOrDefault("userAccount", "用户" + userId)));
            fallback.setUserRole(String.valueOf(claims.getOrDefault("userRole", "user")));
            return fallback;
        } catch (Exception e) {
            log.warn("WebSocket JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }
}
