package com.ellu.looper.notification.dto;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder(toBuilder = true)
public class NotificationDto {
  private Long id;
  private Long senderId;
  private Long receiverId;
  private Long projectId;
  private Long templateId;
  private Map<String, Object> payload;
  private String inviteStatus;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
