package com.ellu.looper.kafka.service;

import com.ellu.looper.sse.service.ChatSseService;
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
  private final ChatSseService chatSseService;

  @Bean
  public Consumer<KStream<String, String>> processChatResponse() {
    return stream ->
        stream.foreach(
            (userId, token) -> {
              log.debug("Processing response for user {}: {}", userId, token);
              chatSseService.sendTokenToUser(userId, token, false);
            });
  }
}
