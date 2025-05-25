package com.ellu.looper.controller;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.CurrentUser;
import com.ellu.looper.dto.NotificationDto;
import com.ellu.looper.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  public ApiResponse<List<NotificationDto>> getNotifications(@CurrentUser Long userId) {
    return ApiResponse.success("notifications_fetched",notificationService.getNotifications(userId));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<?> markAsRead(@PathVariable Long id, @CurrentUser Long userId) {
    notificationService.markAsRead(id, userId);
    return ResponseEntity.ok().build();
  }

}
