package com.ellu.looper.sse.controller;

import com.ellu.looper.sse.dto.SseMessage;
import com.ellu.looper.sse.service.SseService;
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

  private final SseService sseService;

  @Value("${sse.internal.key:internal-sse-key}")
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

      // 로컬 사용자에게 직접 전송
      sseService.sendToLocalUserDirectly(
          message.getUserId(), message.getEventName(), message.getData());

      return ResponseEntity.ok("Message forwarded successfully");
    } catch (Exception e) {
      log.error("Failed to forward message for user {}", message.getUserId(), e);
      return ResponseEntity.internalServerError().body("Failed to forward message");
    }
  }
}
