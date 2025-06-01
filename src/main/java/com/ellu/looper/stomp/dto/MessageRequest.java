package com.ellu.looper.stomp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {
  private String message;
  private String nickname;
}
