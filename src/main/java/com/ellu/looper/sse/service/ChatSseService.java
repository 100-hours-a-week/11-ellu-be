package com.ellu.looper.sse.service;

import com.ellu.looper.sse.dto.SsePubSubMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;

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
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    emitters.put(sessionId, emitter);
    log.info("SessionId {} is connected to chat stream.", sessionId);

    emitter.onCompletion(
        () -> {
          log.info("SSE completed for session: {}", sessionId);
          emitters.remove(sessionId);
          unregisterSession(userId);
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
    redisTemplate.opsForValue().set(key, sessionId, 1, TimeUnit.HOURS); // 1시간 라우팅 타임아웃
    log.info("Registered session {} to routing.", userId);
  }

  public void unregisterSession(Long userId) {
    String key = routingKeyPrefix + userId;
    redisTemplate.delete(key);
    log.info("Unregistered session {} from routing.", userId);
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
        log.debug("Sent message to local session {}: {} - {}", sessionId, eventName, data);
        return;
      }
      log.warn("No emitter found for local session {}", sessionId);
      throw new IllegalStateException("No emitter found for local session " + sessionId);
    } catch (Exception e) {
      log.error("Error sending message to local session {}", sessionId, e);
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
    return sessionId.equals(redisTemplate.opsForValue().get(routingKeyPrefix + userId));
  }

  private void forwardToSession(String targetSessionId, String eventName, String data) {
    try {
      // Redis Pub/Sub을 통해 메시지 전달
      SsePubSubMessage message = new SsePubSubMessage(targetSessionId, eventName, data);
      redisTemplate.convertAndSend(SSE_CHANNEL, message);
      log.info("Published SSE message to channel {} for session {}", SSE_CHANNEL, targetSessionId);
    } catch (Exception e) {
      log.error("Error publishing SSE message to channel {}: {}", SSE_CHANNEL, e.getMessage());
    }
  }

  // userId로부터 sessionId를 조회해 메시지 전송
  public void sendTokenToUser(String userId, String token, boolean done) {
    String sessionId = (String) redisTemplate.opsForValue().get(routingKeyPrefix + userId);
    if (sessionId != null) {
      sendToken(Long.valueOf(userId), sessionId, token, done);
    } else {
      log.warn("No sessionId found for userId {} when trying to send message", userId);
    }
  }


  public void sendToken(Long userId, String sessionId, String token, boolean done) {
    try {
      String targetSessionId = getTargetSession(userId);
      if (targetSessionId == null) {
        log.warn("No routing information found for session {}", sessionId);
        return;
      }
      if (isCurrentSession(userId, targetSessionId)) {

        SseEmitter localEmitter = getEmitter(sessionId);
        if (localEmitter != null) {
          sendToLocalSession(sessionId,"token", "{\"token\":\"" + token + "\",\"done\":" + done + "}");
        }
      } else {
        forwardToSession(targetSessionId, "token","{\"token\":\"" + token + "\",\"done\":" + done + "}");
      }
    } catch (Exception e) {
      log.error("Error sending message to session {}: {}", sessionId, e.getMessage());
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
      log.warn("No sessionId found for userId {} when trying to send message", userId);
    }
  }

  public void sendMessage(Long userId, String sessionId, String message, boolean done) {
    try {
      String targetSessionId = getTargetSession(userId);
      if (targetSessionId == null) {
        log.warn("No routing information found for session {}", sessionId);
        return;
      }
      if (isCurrentSession(userId, targetSessionId)) {

        SseEmitter localEmitter = getEmitter(sessionId);
        if (localEmitter != null) {
          sendToLocalSession(sessionId,"message", "{\"message\":\"" + message + "\",\"done\":" + done + "}");
        }
      } else {
        forwardToSession(targetSessionId, "message","{\"message\":\"" + message + "\",\"done\":" + done + "}");
      }
    } catch (Exception e) {
      log.error("Error sending message to session {}: {}", sessionId, e.getMessage());
      removeEmitter(sessionId);
      unregisterSession(userId);
    }
  }

  public void sendSchedulePreview(
      String userId,
      String taskTitle,
      String category,
      String subtaskTitle,
      String startTime,
      String endTime,
      boolean done) {
    SseEmitter emitter = emitters.get(userId);
    if (emitter != null) {
      try {
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

        emitter.send(
            SseEmitter.event()
                .name("schedule") // SSE 이벤트 이름은 "schedule"
                .data(rootNode.toString())); // JsonNode를 String으로 변환하여 전송
      } catch (IOException e) {
        log.error("Error sending SSE to user: {}", userId, e);
        emitters.remove(userId);
      }
    }
  }
}
