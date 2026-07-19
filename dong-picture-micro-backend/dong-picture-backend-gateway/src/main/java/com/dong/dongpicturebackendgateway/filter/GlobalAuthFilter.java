package com.dong.dongpicturebackendgateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class GlobalAuthFilter implements GlobalFilter, Ordered {

    private static final String JWT_SECRET = "dong-picture-jwt-secret-key-change-me";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final List<String> SKIP_AUTH_PATHS = List.of(
            "/api/user/login",
            "/api/user/register",
            "/api/doc.html",
            "/api/v2/api-docs",
            "/api/swagger",
            "/api/webjars",
            "/api/health",
            "/api/picture/tag_category",
            "/api/picture/hot"
    );

    private static final List<String> SOFT_CHECK_PATHS = List.of(
            "/api/picture/list/page/vo",
            "/api/picture/get/vo",
            "/api/user/get/vo",
            "/api/user/get/login"
    );

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Block external access to /**/inner/**
        if (path.contains("/inner/")) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return response.setComplete();
        }

        // Skip auth for whitelisted paths
        if (matchesAny(path, SKIP_AUTH_PATHS)) {
            return chain.filter(exchange);
        }

        // Soft-check: pass with anonymous if no token
        boolean isSoftCheck = matchesAny(path, SOFT_CHECK_PATHS);

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            if (isSoftCheck) {
                return chain.filter(exchange);
            }
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            Claims claims = parseJwt(token);

            String userId = String.valueOf(claims.get("userId"));
            String userAccount = String.valueOf(claims.get("userAccount"));
            String userRole = String.valueOf(claims.get("userRole"));

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Account", userAccount)
                    .header("X-User-Role", userRole)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            if (isSoftCheck) {
                return chain.filter(exchange);
            }
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
    }

    private Claims parseJwt(String token) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean matchesAny(String path, List<String> patterns) {
        for (String pattern : patterns) {
            if (path.startsWith(pattern)) {
                return true;
            }
        }
        return false;
    }
}
