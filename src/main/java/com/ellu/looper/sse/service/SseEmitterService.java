package com.ellu.looper.sse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class SseEmitterService {
  private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();

  public SseEmitter createEmitter(String userId) {
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    log.info("USER WITH ID {} IS CONNECTED TO SSE", userId);

    emitter.onCompletion(
        () -> {
          log.info("SSE completed for user: {}", userId);
          emitters.remove(userId);
        });

    emitter.onTimeout(
        () -> {
          log.info("SSE timeout for user: {}", userId);
          emitters.remove(userId);
        });

    emitter.onError(
        e -> {
          log.error("SSE error for user: {}", userId, e);
          emitters.remove(userId);
        });

    emitters.put(userId, emitter);
    return emitter;
  }

  public void sendToken(String userId, String token, boolean done) {
    SseEmitter emitter = emitters.get(userId);
    if (emitter != null) {
      try {
        emitter.send(
            SseEmitter.event()
                .name("token")
                .data("{\"token\":\"" + token + "\",\"done\":" + done + "}"));
      } catch (IOException e) {
        log.error("Error sending SSE to user: {}", userId, e);
        emitters.remove(userId);
      }
    }
  }

  public void sendMessage(String userId, String message, boolean done) {
    SseEmitter emitter = emitters.get(userId);
    if (emitter != null) {
      try {
        emitter.send(
            SseEmitter.event()
                .name("message")
                .data("{\"message\":\"" + message + "\",\"done\":" + done + "}"));
      } catch (IOException e) {
        log.error("Error sending SSE to user: {}", userId, e);
        emitters.remove(userId);
      }
    }
  }

  public void sendSchedulePreview(
      String userId,
      String taskTitle,
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
