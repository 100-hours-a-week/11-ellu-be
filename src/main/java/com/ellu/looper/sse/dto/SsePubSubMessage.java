package com.ellu.looper.sse.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SsePubSubMessage implements Serializable {
    private String targetSessionId;
    private String eventName;
    private String data;
} 