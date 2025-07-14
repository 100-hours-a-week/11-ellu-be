package com.ellu.looper.sse.service;

import com.ellu.looper.sse.dto.SsePubSubMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SsePubSubListener implements MessageListener {
    private final ChatSseService chatSseService;
    private final NotificationSseService notificationSseService;
    private final Jackson2JsonRedisSerializer<SsePubSubMessage> serializer = new Jackson2JsonRedisSerializer<>(SsePubSubMessage.class);

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            SsePubSubMessage pubSubMessage = serializer.deserialize(message.getBody());
            if (pubSubMessage == null) return;
            String sessionId = pubSubMessage.getTargetSessionId();
            if (chatSseService.getEmitter(sessionId) != null) {
                chatSseService.sendToLocalSession(sessionId, pubSubMessage.getEventName(), pubSubMessage.getData());
                log.info("Delivered chat SSE message to local session {} via PubSub", sessionId);
            }
            if (notificationSseService.getEmitter(sessionId) != null) {
                notificationSseService.sendToLocalSession(sessionId, pubSubMessage.getEventName(), pubSubMessage.getData());
                log.info("Delivered notification SSE message to local session {} via PubSub", sessionId);
            }
        } catch (Exception e) {
            log.error("Failed to process SSE PubSub message", e);
        }
    }
} 