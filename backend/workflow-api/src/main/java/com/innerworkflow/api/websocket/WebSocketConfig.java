package com.innerworkflow.api.websocket;

import com.innerworkflow.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ApprovalWebSocketHandler approvalWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(approvalWebSocketHandler, "/ws/approval")
                .addInterceptors(webSocketHandshakeInterceptor())
                .setAllowedOriginPatterns("*");
    }

    @Bean
    public HandshakeInterceptor webSocketHandshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                           WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                Long userId = null;
                try {
                    userId = SecurityUtils.getCurrentUserIdOrNull();
                } catch (Exception ignored) {}

                if (userId == null && request instanceof ServletServerHttpRequest) {
                    String token = ((ServletServerHttpRequest) request).getServletRequest().getParameter("token");
                    if (token != null && !token.isEmpty()) {
                        log.debug("WebSocket连接携带token参数");
                    }
                }

                if (userId != null) {
                    attributes.put("userId", userId);
                    log.debug("WebSocket握手成功, userId={}", userId);
                    return true;
                }
                log.warn("WebSocket握手失败: 用户未认证");
                return false;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Exception exception) {
            }
        };
    }
}
