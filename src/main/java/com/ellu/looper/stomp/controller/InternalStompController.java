package com.ellu.looper.stomp.controller;

import com.ellu.looper.stomp.dto.StompMessage;
import com.ellu.looper.stomp.service.StompService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/stomp")
@RequiredArgsConstructor
public class InternalStompController {
  private final StompService stompService;

  @Value("${stomp.internal.key}")
  private String internalKey;

  @PostMapping("/forward")
  public ResponseEntity<String> forwardMessage(
      @RequestBody StompMessage message,
      @RequestHeader(value = "X-Internal-Key", required = false) String internalKey) {
    if (!this.internalKey.equals(internalKey)) {
      return ResponseEntity.status(401).body("Unauthorized");
    }
    try {
      stompService.sendToLocalUser(message.getDestination(), message.getPayload());
      return ResponseEntity.ok("Message forwarded successfully");
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("Failed to forward message");
    }
  }
}
