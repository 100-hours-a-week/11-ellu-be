package com.ellu.looper.kafka;

import com.ellu.looper.chat.dto.MessageRequest;
import com.ellu.looper.fastapi.service.FastApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatConsumer {
  private final FastApiService fastApiService;
  private final ChatProducer chatProducer;

  @KafkaListener(topics = "chatbot-user-input", groupId = "chat-group")
  public void consumeUserMessage(String userId, MessageRequest message) {
    log.info("Received message from user {}: {}", userId, message);

    fastApiService
        .streamChatResponse(message)
        .doOnNext(
            token -> {
              chatProducer.sendResponseToken(Long.parseLong(userId), token, false);
            })
        .doOnComplete(
            () -> {
              chatProducer.sendResponseToken(Long.parseLong(userId), "", true);
            })
        .doOnError(
            error -> {
              log.error("Error processing chat message: {}", error.getMessage());
              chatProducer.sendResponseToken(
                  Long.parseLong(userId), "Error: " + error.getMessage(), true);
            })
        .subscribe();
  }
}
