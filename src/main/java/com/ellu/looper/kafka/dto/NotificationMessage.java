package com.ellu.looper.kafka.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
  private String type; // e.g., "INVITE"
  private Long projectId;
  private Long senderId;
  private List<Long> receiverId;
  private String message;
}
