package com.innerworkflow.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innerworkflow.common.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ApprovalWebSocketHandler extends TextWebSocketHandler {

    private static final ConcurrentHashMap<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ConcurrentHashMap<Long, WebSocketSession>> instanceSubscribers = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public ApprovalWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.put(userId, session);
            log.info("WebSocket连接建立, userId={}, sessionId={}", userId, session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Long userId = getUserIdFromSession(session);

        try {
            WebSocketMessage msg = objectMapper.readValue(payload, WebSocketMessage.class);
            if ("subscribe".equals(msg.getType()) && msg.getInstanceNo() != null) {
                instanceSubscribers.computeIfAbsent(msg.getInstanceNo(), k -> new ConcurrentHashMap<>())
                        .put(userId, session);
                log.info("用户{}订阅审批单{}, sessionId={}", userId, msg.getInstanceNo(), session.getId());
            } else if ("unsubscribe".equals(msg.getType()) && msg.getInstanceNo() != null) {
                ConcurrentHashMap<Long, WebSocketSession> subscribers = instanceSubscribers.get(msg.getInstanceNo());
                if (subscribers != null) {
                    subscribers.remove(userId);
                    if (subscribers.isEmpty()) {
                        instanceSubscribers.remove(msg.getInstanceNo());
                    }
                }
                log.info("用户{}取消订阅审批单{}, sessionId={}", userId, msg.getInstanceNo(), session.getId());
            }
        } catch (Exception e) {
            log.warn("处理WebSocket消息失败, userId={}, payload={}, error={}", userId, payload, e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.remove(userId);
            instanceSubscribers.forEach((instanceNo, subscribers) -> {
                subscribers.remove(userId);
                if (subscribers.isEmpty()) {
                    instanceSubscribers.remove(instanceNo);
                }
            });
            log.info("WebSocket连接关闭, userId={}, sessionId={}", userId, session.getId());
        }
    }

    public void pushStatusUpdate(String instanceNo, ApprovalStatusUpdateVO updateVO) {
        ConcurrentHashMap<Long, WebSocketSession> subscribers = instanceSubscribers.get(instanceNo);
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(updateVO);
            TextMessage message = new TextMessage(json);
            subscribers.values().forEach(session -> {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        log.warn("推送WebSocket消息失败, sessionId={}, error={}", session.getId(), e.getMessage());
                    }
                }
            });
            log.debug("已向{}个订阅者推送审批单{}状态更新", subscribers.size(), instanceNo);
        } catch (Exception e) {
            log.warn("序列化WebSocket消息失败, instanceNo={}, error={}", instanceNo, e.getMessage());
        }
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        Object userIdAttr = session.getAttributes().get("userId");
        if (userIdAttr instanceof Long) {
            return (Long) userIdAttr;
        }
        try {
            return SecurityUtils.getCurrentUserIdOrNull();
        } catch (Exception e) {
            return null;
        }
    }

    @lombok.Data
    public static class WebSocketMessage {
        private String type;
        private String instanceNo;
    }
}
