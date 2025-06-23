package com.ellu.looper.sse.service;

import com.ellu.looper.kafka.dto.NotificationMessage;
import com.ellu.looper.notification.dto.NotificationDto;
import com.ellu.looper.notification.entity.Notification;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@Slf4j
public class SseService {

  private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

  public SseEmitter subscribe(Long userId) {
    SseEmitter emitter = new SseEmitter(60L * 1000 * 60); // 60분 타임아웃
    emitters.put(userId, emitter);
    log.info("UserId {} is connected to sse. ", userId);

    emitter.onCompletion(
        () -> {
          emitters.remove(userId);
          log.info("USER WITH ID {} IS DISCONNECTED TO SSE", userId);
        });
    emitter.onTimeout(
        () -> {
          emitters.remove(userId);
          log.info("USER WITH ID {} IS CONNECTION TIMEOUT", userId);
        });

    return emitter;
  }

  public void sendNotification(Long userId, NotificationMessage dto) {
    SseEmitter emitter = emitters.get(userId);
    if (emitter != null) {
      try {

        // 로그 출력 추가
        log.info("Sending SSE notification to user {}: {}", userId, dto);

        emitter.send(SseEmitter.event().name("notification").data(dto));
      } catch (IOException e) {
        log.warn("Failed to send SSE to user {}. Removing emitter.", userId, e);
        emitters.remove(userId);
      }
    } else {
      log.info("No active emitter for user {}", userId);
    }
  }

  private NotificationDto notificationToDto(Notification notification) {
    return NotificationDto.builder()
        .id(notification.getId())
        .message(renderTemplate(notification.getTemplate().getTemplate(), notification))
        .build();
  }

  private String renderTemplate(String template, Notification notification) {
    // 실제 초대 메시지로 변환: "{{creator}}님이 {{project}}에 초대했습니다."
    return template
        .replace("{{creator}}", notification.getSender().getNickname())
        .replace("{{project}}", notification.getProject().getTitle());
  }
}
