package com.ellu.looper.stomp.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StompPubSubMessage implements Serializable {
  private String targetSessionId;
  private String eventName;
  private String data;
  private String projectId;
}
