package com.innerworkflow.form.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DictChangeWebSocketNotifier {

    private static final ConcurrentHashMap<Long, WebSocketSession> dictSubscribers = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public void registerSession(Long userId, WebSocketSession session) {
        dictSubscribers.put(userId, session);
    }

    public void removeSession(Long userId) {
        dictSubscribers.remove(userId);
    }

    @EventListener
    public void onDictChange(DictChangeEvent event) {
        if (dictSubscribers.isEmpty()) {
            return;
        }

        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "dictChange");
            message.put("dictCode", event.getDictCode());
            message.put("changeType", event.getChangeType());
            message.put("timestamp", System.currentTimeMillis());

            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);

            dictSubscribers.values().forEach(session -> {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.warn("推送字典变更WebSocket消息失败, sessionId={}, error={}", session.getId(), e.getMessage());
                    }
                }
            });
            log.debug("已向{}个订阅者推送字典{}变更通知", dictSubscribers.size(), event.getDictCode());
        } catch (Exception e) {
            log.warn("序列化字典变更消息失败, dictCode={}, error={}", event.getDictCode(), e.getMessage());
        }
    }
}
