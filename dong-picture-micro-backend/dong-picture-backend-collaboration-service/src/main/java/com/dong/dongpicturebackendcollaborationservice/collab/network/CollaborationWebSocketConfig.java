package com.dong.dongpicturebackendcollaborationservice.collab.network;

import com.dong.dongpicturebackendcollaborationservice.collab.engine.OperationLog;
import com.dong.dongpicturebackendcollaborationservice.collab.sync.CollaborationSession;
import com.dong.dongpicturebackendcollaborationservice.collab.sync.PresenceTracker;
import com.dong.dongpicturebackendcollaborationservice.collab.sync.SyncStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;
import java.util.Map;

/**
 * 协作 WebSocket 配置类。
 *
 * 注册 WebSocket 端点 /ws/collab/picture，
 * 并配置定时任务清理超时的 Presence 会话。
 */
@Configuration
@EnableWebSocket
@EnableScheduling
public class CollaborationWebSocketConfig implements WebSocketConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CollaborationWebSocketConfig.class);

    /** 心跳超时：30 秒未收到心跳认为客户端离线 */
    private static final long HEARTBEAT_TIMEOUT_MS = 30_000;

    /** 清理任务间隔：每 15 秒 */
    private static final long CLEANUP_INTERVAL_MS = 15_000;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 协作编辑通道
        registry.addHandler(webSocketTransport(), "/ws/collab/picture")
                .addInterceptors(new CollabHandshakeInterceptor())
                .setAllowedOrigins("*");
        // WebRTC 信令通道
        registry.addHandler(webRTCSignalingHandler(), "/ws/collab/signaling")
                .addInterceptors(new CollabHandshakeInterceptor())
                .setAllowedOrigins("*");
    }

    @Bean
    public CollaborationSession collaborationSession() {
        return new CollaborationSession();
    }

    @Bean
    public PresenceTracker presenceTracker() {
        return new PresenceTracker();
    }

    @Bean
    public SyncStateManager syncStateManager(OperationLog operationLog) {
        return new SyncStateManager(operationLog);
    }

    @Bean
    public WebSocketTransport webSocketTransport() {
        return new WebSocketTransport(
                collaborationSession(),
                presenceTracker(),
                syncStateManager(operationLog()),
                operationLog()
        );
    }

    @Bean
    public WebRTCSignalingHandler webRTCSignalingHandler() {
        return new WebRTCSignalingHandler();
    }

    @Bean
    public OperationLog operationLog() {
        // 默认使用内存实现，生产环境切换到 Redis Streams
        return new com.dong.dongpicturebackendcollaborationservice.collab.engine.InMemoryOperationLog();
    }

    /**
     * 定时清理超时的 Presence 会话
     */
    @Scheduled(fixedRate = CLEANUP_INTERVAL_MS)
    public void evictStalePresence() {
        List<Map.Entry<Long, String>> evicted = presenceTracker().evictStaleSessions(HEARTBEAT_TIMEOUT_MS);
        for (Map.Entry<Long, String> entry : evicted) {
            Long pictureId = entry.getKey();
            String clientId = entry.getValue();
            log.info("Evicted stale presence: pictureId={}, clientId={}", pictureId, clientId);

            // 通知其他客户端该用户离线
            com.dong.dongpicturebackendcollaborationservice.collab.network.CollabMessage leaveMsg =
                    new com.dong.dongpicturebackendcollaborationservice.collab.network.CollabMessage();
            leaveMsg.setType(CollabMessageType.CLIENT_LEAVE);
            leaveMsg.setPictureId(pictureId);
            leaveMsg.setClientId(clientId);
            leaveMsg.setMessage("用户 " + clientId + " 已离线");
            // Note: broadcastToPicture is in WebSocketTransport, handle via event or flag
        }
    }
}
