package com.ellu.looper.sse.service;

import com.ellu.looper.sse.dto.SsePubSubMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SsePubSubListener implements MessageListener {
  private final ChatSseService chatSseService;
  private final NotificationSseService notificationSseService;
  private final GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

  @Override
  public void onMessage(Message message, byte[] pattern) {
    try {
      SsePubSubMessage pubSubMessage = (SsePubSubMessage) serializer.deserialize(message.getBody());
      if (pubSubMessage == null) return;
      String sessionId = pubSubMessage.getTargetSessionId();
      String eventName = pubSubMessage.getEventName();

      if ("message".equals(eventName) || "token".equals(eventName) || "schedule".equals(eventName)) {
        if (chatSseService.getEmitter(sessionId) != null) {
          chatSseService.sendToLocalSession(sessionId, eventName, pubSubMessage.getData());
          log.info("[CHAT SSE] Delivered chat SSE message to local session {} via PubSub", sessionId);
        }
      } else if ("notification".equals(eventName)) {
        if (notificationSseService.getEmitter(sessionId) != null) {
          notificationSseService.sendToLocalSession(sessionId, eventName, pubSubMessage.getData());
          log.info("[NOTIFICATION SSE] Delivered notification SSE message to local session {} via PubSub", sessionId);
        }
      }
    } catch (Exception e) {
      log.error("[SSE] Failed to process SSE PubSub message", e);
    }
  }
}
