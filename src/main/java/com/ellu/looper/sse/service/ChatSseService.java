package com.ellu.looper.sse.service;

import com.ellu.looper.sse.dto.SsePubSubMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSseService {

  private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final RedisTemplate<String, Object> redisTemplate;
  private static final String routingKeyPrefix = "sse:routing:chat:";
  private static final String SSE_CHANNEL = "sse:events";

  public SseEmitter createEmitter(HttpServletRequest request, Long userId) {
    String sessionId = request.getSession().getId();

    SseEmitter old = emitters.remove(sessionId);
    if (old != null) old.complete();

    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    emitters.put(sessionId, emitter);
    log.info("[CHAT SSE] SessionId {} is connected to chat stream.", sessionId);

    emitter.onCompletion(
        () -> {
          log.info("[CHAT SSE] SSE completed for session: {}", sessionId);
          emitters.remove(sessionId);
          unregisterSession(userId);
        });

    emitter.onTimeout(
        () -> {
          emitters.remove(sessionId);
          unregisterSession(userId);
          log.info("[CHAT SSE] SSE timeout for session: {}", sessionId);
        });

    emitter.onError(
        e -> {
          log.error("[CHAT SSE] SSE error for session: {}", sessionId, e);
          emitters.remove(sessionId);
          unregisterSession(userId);
        });
    registerSession(userId, sessionId);
    return emitter;
  }

  public void registerSession(Long userId, String sessionId) {
    String key = routingKeyPrefix + userId;
    redisTemplate.opsForValue().set(key, sessionId, 1, TimeUnit.HOURS); // 1시간 라우팅 타임아웃
    log.info("[CHAT SSE] Registered session {} to routing.", userId);
  }

  public void unregisterSession(Long userId) {
    String key = routingKeyPrefix + userId;
    redisTemplate.delete(key);
    log.info("[CHAT SSE] Unregistered session {} from routing.", userId);
  }

  public SseEmitter getEmitter(String sessionId) {
    return emitters.get(sessionId);
  }

  public void removeEmitter(String sessionId) {
    emitters.remove(sessionId);
  }

  public void sendToLocalSession(String sessionId, String eventName, String data) {
    try {
      SseEmitter emitter = getEmitter(sessionId);
      if (emitter != null) {
        emitter.send(SseEmitter.event().name(eventName).data(data));
        log.info("!![CHAT SSE] Sent message to local session {}: {} - {}", sessionId, eventName, data);
        return;
      }
      log.warn("[CHAT SSE] No emitter found for local session {}", sessionId);
      throw new IllegalStateException("No emitter found for local session " + sessionId);
    } catch (Exception e) {
      log.error("[CHAT SSE] Error sending message to local session {}", sessionId, e);
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

  private void forwardToSession(String targetSessionId, String eventName, String data) {
    try {
      // Redis Pub/Sub을 통해 메시지 전달
      SsePubSubMessage message = new SsePubSubMessage(targetSessionId, eventName, data);
      redisTemplate.convertAndSend(SSE_CHANNEL, message);
      log.info("!![CHAT SSE] Published SSE message to channel {} for session {}", SSE_CHANNEL, targetSessionId);
    } catch (Exception e) {
      log.error("[CHAT SSE] Error publishing SSE message to channel {}: {}", SSE_CHANNEL, e.getMessage());
    }
  }

  // userId로부터 sessionId를 조회해 메시지 전송
  public void sendTokenToUser(String userId, String token, boolean done) {
    String sessionId = (String) redisTemplate.opsForValue().get(routingKeyPrefix + userId);
    if (sessionId != null) {
      sendToken(Long.valueOf(userId), sessionId, token, done);
    } else {
      log.warn("[CHAT SSE] No sessionId found for userId {} when trying to send message", userId);
    }
  }

  public void sendToken(Long userId, String sessionId, String token, boolean done) {
    try {
      String targetSessionId = getTargetSession(userId);
      if (targetSessionId == null) {
        log.warn("[CHAT SSE] No routing information found for session {}", sessionId);
        return;
      }
      if (isCurrentSession(userId, sessionId)) {

        SseEmitter localEmitter = getEmitter(sessionId);
        if (localEmitter != null) {
          sendToLocalSession(
              sessionId, "token", "{\"token\":\"" + token + "\",\"done\":" + done + "}");
        }
      } else {
        forwardToSession(
            targetSessionId, "token", "{\"token\":\"" + token + "\",\"done\":" + done + "}");
      }
    } catch (Exception e) {
      log.error("[CHAT SSE] Error sending message to session {}: {}", sessionId, e.getMessage());
      removeEmitter(sessionId);
      unregisterSession(userId);
    }
  }

  // userId로부터 sessionId를 조회해 메시지 전송
  public void sendMessageToUser(Long userId, String message, boolean done) {
    String sessionId = (String) redisTemplate.opsForValue().get(routingKeyPrefix + userId);
    if (sessionId != null) {
      sendMessage(userId, sessionId, message, done);
    } else {
      log.warn("[CHAT SSE] No sessionId found for userId {} when trying to send message", userId);
    }
  }

  public void sendMessage(Long userId, String sessionId, String message, boolean done) {
    try {
      String targetSessionId = getTargetSession(userId);
      if (targetSessionId == null) {
        log.warn("[CHAT SSE] No routing information found for session {}", sessionId);
        return;
      }
      if (isCurrentSession(userId, targetSessionId)) {

        SseEmitter localEmitter = getEmitter(sessionId);
        if (localEmitter != null) {
          sendToLocalSession(
              sessionId, "message", "{\"message\":\"" + message + "\",\"done\":" + done + "}");
        }
      } else {
        forwardToSession(
            targetSessionId, "message", "{\"message\":\"" + message + "\",\"done\":" + done + "}");
      }
    } catch (Exception e) {
      log.error("[CHAT SSE] Error sending message to session {}: {}", sessionId, e.getMessage());
      removeEmitter(sessionId);
      unregisterSession(userId);
    }
  }

  // userId로부터 sessionId를 조회해 메시지 전송
  public void sendSchedulePreviewToUser(
      Long userId,
      String taskTitle,
      String category,
      String subtaskTitle,
      String startTime,
      String endTime,
      boolean done) {
    String sessionId = (String) redisTemplate.opsForValue().get(routingKeyPrefix + userId);
    if (sessionId != null) {
      sendSchedulePreview(
          userId, sessionId, taskTitle, category, subtaskTitle, startTime, endTime, done);
    } else {
      log.warn("[CHAT SSE] No sessionId found for userId {} when trying to send message", userId);
    }
  }

  public void sendSchedulePreview(
      Long userId,
      String sessionId,
      String taskTitle,
      String category,
      String subtaskTitle,
      String startTime,
      String endTime,
      boolean done) {
    try {
      String targetSessionId = getTargetSession(userId);
      if (targetSessionId == null) {
        log.warn("[CHAT SSE] No routing information found for session {}", sessionId);
        return;
      }
      // JSON 객체 생성을 위해 ObjectMapper 사용
      ObjectNode rootNode = objectMapper.createObjectNode();
      rootNode.put("task_title", taskTitle);
      rootNode.put("category", category);

      ArrayNode schedulePreviewArray = objectMapper.createArrayNode();
      ObjectNode subtaskObject = objectMapper.createObjectNode();
      subtaskObject.put("title", subtaskTitle);
      subtaskObject.put("start_time", startTime);
      subtaskObject.put("end_time", endTime);
      schedulePreviewArray.add(subtaskObject);

      rootNode.set("schedule_preview", schedulePreviewArray);
      rootNode.put("done", done); // done 필드를 root 레벨에 추가
      if (isCurrentSession(userId, targetSessionId)) {

        SseEmitter localEmitter = getEmitter(sessionId);
        if (localEmitter != null) {
          sendToLocalSession(sessionId, "schedule", rootNode.toString());
        }
      } else {
        forwardToSession(targetSessionId, "schedule", rootNode.toString());
      }
    } catch (Exception e) {
      log.error("[CHAT SSE] Error sending message to session {}: {}", sessionId, e.getMessage());
      removeEmitter(sessionId);
      unregisterSession(userId);
    }
  }
}
