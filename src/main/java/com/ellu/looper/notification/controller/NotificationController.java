package com.ellu.looper.notification.controller;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.CurrentUser;
import com.ellu.looper.notification.dto.InvitationProcessRequest;
import com.ellu.looper.notification.dto.NotificationDto;
import com.ellu.looper.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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
  public ApiResponse<NotificationDto> respondToInvitation(@PathVariable Long id, @CurrentUser Long userId, @RequestBody @Valid InvitationProcessRequest request) {
    NotificationDto notificationDto = notificationService.respondToInvitation(id, userId, request.getInviteStatus());
    return ApiResponse.success("invitation_response_processed", notificationDto);
  }
}
