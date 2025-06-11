package com.ellu.looper.kafka.service;

import com.ellu.looper.kafka.ChatProducer;
import com.ellu.looper.sse.service.SseEmitterService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatStreamService {
  private final SseEmitterService sseEmitterService;

  @Bean
  public Consumer<KStream<String, ChatProducer.ChatResponseToken>> processChatResponse() {
    return stream ->
        stream.foreach(
            (userId, response) -> {
              log.debug("Processing response for user {}: {}", userId, response);
              sseEmitterService.sendToken(userId, response.token(), response.done());
            });
  }
}
