package com.ellu.looper.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class NotificationDto {

  @JsonProperty("id")
  private Long id;

  @JsonProperty("sender_nickname")
  private String senderNickname;

  @JsonProperty("message")
  private String message;

  @JsonProperty("is_processed")
  private Boolean processed;

  @JsonProperty("invite_status")
  private String inviteStatus;

  @JsonProperty("created_at")
  private LocalDateTime createdAt;
}
