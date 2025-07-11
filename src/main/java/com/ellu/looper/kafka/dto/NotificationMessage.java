package com.ellu.looper.kafka.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class NotificationMessage {
  private String type;
  private Long notificationId;
  private Long projectId;
  private Long senderId;
  private List<Long> receiverId;
  private String message;
  private Long templateId;
  private Map<String, Object> payload;
  private String inviteStatus;
}
