package com.ellu.looper.stomp.service;

import com.ellu.looper.stomp.dto.StompPubSubMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompPubSubListener implements MessageListener {
  private final StompSessionRoutingService stompSessionRoutingService;
  private final GenericJackson2JsonRedisSerializer serializer =
      new GenericJackson2JsonRedisSerializer();

  @Override
  public void onMessage(Message message, byte[] pattern) {
    try {
      StompPubSubMessage pubSubMessage =
          (StompPubSubMessage) serializer.deserialize(message.getBody());
      if (pubSubMessage == null) return;
      String sessionId = pubSubMessage.getTargetSessionId();
      stompSessionRoutingService.sendToLocalSession(
          sessionId,
          pubSubMessage.getEventName(),
          pubSubMessage.getData(),
          pubSubMessage.getProjectId());
      log.info(
          "[STOMP] Received pubsub message for session {}: event={}, data={}",
          sessionId,
          pubSubMessage.getEventName(),
          pubSubMessage.getData());
    } catch (Exception e) {
      log.error("Failed to process STOMP PubSub message", e);
    }
  }
}
