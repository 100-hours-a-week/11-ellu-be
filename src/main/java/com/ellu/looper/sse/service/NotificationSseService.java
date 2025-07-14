package com.ellu.looper.sse.service;

import com.ellu.looper.kafka.dto.NotificationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.ellu.looper.sse.dto.SsePubSubMessage;
import org.springframework.data.redis.core.RedisTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationSseService {

  private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private static final String SSE_CHANNEL = "sse:events";
  private static final String routingKeyPrefix = "sse:routing:notification:";

  public SseEmitter subscribe(HttpServletRequest request, Long userId) {
    String sessionId = request.getSession().getId();
    SseEmitter emitter = new SseEmitter(60L * 1000 * 60); // 60분 타임아웃
    emitters.put(sessionId, emitter);
    log.info("SessionId {} is connected to notification sse.", sessionId);

    emitter.onCompletion(
        () -> {
          emitters.remove(sessionId);
          unregisterSession(userId);
          log.info("SessionId {} is disconnected from notification sse.", sessionId);
        });

    emitter.onTimeout(
        () -> {
          emitters.remove(sessionId);
          unregisterSession(userId);
          log.info("SSE timeout for session: {}", sessionId);
        });

    emitter.onError(
        e -> {
          log.error("SSE error for session: {}", sessionId, e);
          emitters.remove(sessionId);
          unregisterSession(userId);
        });
    registerSession(userId, sessionId);
    return emitter;
  }

  public void registerSession(Long userId, String sessionId) {
    String key = routingKeyPrefix + userId;
    redisTemplate.opsForValue().set(key, sessionId, 1, java.util.concurrent.TimeUnit.HOURS);
    log.info("Registered session {} to notification routing.", userId);
  }

  public void unregisterSession(Long userId) {
    String key = routingKeyPrefix + userId;
    redisTemplate.delete(key);
    log.info("Unregistered session {} from notification routing.", userId);
  }

  public SseEmitter getEmitter(String sessionId) {
    return emitters.get(sessionId);
  }

  public void removeEmitter(String sessionId) {
    emitters.remove(sessionId);
  }

  public void sendToLocalSession(String sessionId, String eventName, String data) {
    try {
      NotificationMessage notificationMessage = objectMapper.readValue(data, NotificationMessage.class);
      SseEmitter emitter = getEmitter(sessionId);
      if (emitter != null) {
        emitter.send(SseEmitter.event().name(eventName).data(notificationMessage));
        log.info("Sent notification to local session {}: {} - {}", sessionId, eventName, data);
        return;
      }
      log.warn("No emitter found for local session {}", sessionId);
      throw new IllegalStateException("No emitter found for local session " + sessionId);
    } catch (Exception e) {
      log.error("Error sending notification to local session {}: {}", sessionId, e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  private String getTargetSession(Long userId) {
    String key = routingKeyPrefix + userId;
    Object value = redisTemplate.opsForValue().get(key);
    if (value instanceof String) {
      return (String) value;
    }
    return null;
  }

  private boolean isCurrentSession(Long userId, String sessionId) {
    Object redisSession = redisTemplate.opsForValue().get(routingKeyPrefix + userId);
    return sessionId.equals(redisSession) && emitters.containsKey(sessionId);
  }

  private void forwardToSession(String targetSessionId, String eventName,
      NotificationMessage data) {
    try {
      String jsonData = objectMapper.writeValueAsString(data);
      SsePubSubMessage message = new SsePubSubMessage(targetSessionId, eventName, jsonData);
      redisTemplate.convertAndSend(SSE_CHANNEL, message);
      log.info("Published notification SSE message to channel {} for session {}", SSE_CHANNEL,
          targetSessionId);
    } catch (Exception e) {
      log.error("Error publishing notification SSE message to channel {}: {}", SSE_CHANNEL,
          e.getMessage());
    }
  }



  // userId로부터 sessionId를 조회해 메시지 전송
  public void sendNotificationToUser(Long userId, NotificationMessage dto) {
    String sessionId = (String) redisTemplate.opsForValue().get(routingKeyPrefix + userId);
    if (sessionId != null) {
      sendNotification(userId, sessionId, dto);
    } else {
      log.warn("No sessionId found for userId {} when trying to send message", userId);
    }
  }

  public void sendNotification(Long userId, String sessionId, NotificationMessage dto) {
    try {
      String targetSessionId = getTargetSession(userId);
      if (targetSessionId == null) {
        log.warn("No routing information found for session {}", sessionId);
        return;
      }
      if (isCurrentSession(userId, sessionId)) {
        SseEmitter localEmitter = getEmitter(sessionId);
        if (localEmitter != null) {
          String msg = objectMapper.writeValueAsString(dto);
          sendToLocalSession(sessionId, "notification", msg);
        }
      } else {
        forwardToSession(targetSessionId, "notification", dto);
      }
    } catch (Exception e) {
      log.error("Error sending notification message to session {}: {}", sessionId, e.getMessage());
      removeEmitter(sessionId);
      unregisterSession(userId);
    }
  }
}
