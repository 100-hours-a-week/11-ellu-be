package com.ellu.looper.sse.controller;

import com.ellu.looper.sse.dto.SseMessage;
import com.ellu.looper.sse.service.ChatSseService;
import com.ellu.looper.sse.service.NotificationSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/sse")
public class InternalSseController {

  private final NotificationSseService notificationSseService;
  private final ChatSseService chatSseService;

  @Value("${sse.internal.key}")
  private String internalKey;

  /** 다른 Pod에서 전달받은 메시지를 로컬 사용자에게 전송 */
  @PostMapping("/forward")
  public ResponseEntity<String> forwardMessage(
      @RequestBody SseMessage message,
      @RequestHeader(value = "X-Internal-Key", required = false) String internalKey) {

    // 내부 통신 인증 (간단한 키 검증)
    if (!this.internalKey.equals(internalKey)) {
      log.warn("Unauthorized internal SSE request");
      return ResponseEntity.status(401).body("Unauthorized");
    }

    try {
      log.debug(
          "Received forwarded message for user {}: {} - {}",
          message.getUserId(),
          message.getEventName(),
          message.getData());

      // 이벤트 타입에 따라 적절한 서비스 선택
      String eventName = message.getEventName();
      if ("notification".equals(eventName)) {
        // 알림 이벤트는 NotificationSseService 사용
        notificationSseService.sendToLocalUser(
            message.getUserId(), message.getEventName(), message.getData());
      } else {
        // 채팅 관련 이벤트(token, message, schedule)는 ChatSseService 사용
        chatSseService.sendToLocalUser(
            message.getUserId(), message.getEventName(), message.getData());
      }

      return ResponseEntity.ok("Message forwarded successfully");
    } catch (Exception e) {
      log.error("Failed to forward message for user {}", message.getUserId(), e);
      return ResponseEntity.internalServerError().body("Failed to forward message");
    }
  }
}
