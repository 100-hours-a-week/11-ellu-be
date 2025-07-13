package com.ellu.looper.commons;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodInfo {
  private String podId;
  private String host;
  private int port;
  private long connectedAt;
}
