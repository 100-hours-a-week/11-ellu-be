package com.ellu.looper.sse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SseMessage {
  private String userId;
  private String eventName;
  private String data;
  private boolean done;
}
