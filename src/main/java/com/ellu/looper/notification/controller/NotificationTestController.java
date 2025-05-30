package com.ellu.looper.notification.controller;

import com.ellu.looper.commons.enums.NotificationType;
import com.ellu.looper.kafka.NotificationProducer;
import com.ellu.looper.kafka.dto.NotificationMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationTestController {

  private final NotificationProducer notificationProducer;

  @PostMapping("/test/notify")
  public ResponseEntity<String> sendTestNotification(@RequestParam String message) {
    NotificationMessage notificationMessage =
        new NotificationMessage(
            NotificationType.PROJECT_INVITED.toString(), 1L, 1L, List.of(1L), message);
    notificationProducer.sendNotification(notificationMessage);
    return ResponseEntity.ok("Message sent: " + message);
  }
}
