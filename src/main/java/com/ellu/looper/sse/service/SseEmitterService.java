package com.ellu.looper.sse.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class SseEmitterService {
  private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

  public SseEmitter createEmitter(String userId) {
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

    emitter.onCompletion(
        () -> {
          log.info("SSE completed for user: {}", userId);
          emitters.remove(userId);
        });

    emitter.onTimeout(
        () -> {
          log.info("SSE timeout for user: {}", userId);
          emitters.remove(userId);
        });

    emitter.onError(
        e -> {
          log.error("SSE error for user: {}", userId, e);
          emitters.remove(userId);
        });

    emitters.put(userId, emitter);
    return emitter;
  }

  public void sendToken(String userId, String token, boolean done) {
    SseEmitter emitter = emitters.get(userId);
    if (emitter != null) {
      try {
        if (done) {
          emitter.send(SseEmitter.event().name("done").data("{}"));
        } else {
          emitter.send(
              SseEmitter.event()
                  .name("message")
                  .data("{\"token\":\"" + token + "\",\"done\":false}"));
        }
      } catch (IOException e) {
        log.error("Error sending SSE to user: {}", userId, e);
        emitters.remove(userId);
      }
    }
  }
}
