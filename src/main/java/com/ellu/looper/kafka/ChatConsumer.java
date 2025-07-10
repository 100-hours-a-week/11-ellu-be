package com.ellu.looper.kafka;

import com.ellu.looper.chat.dto.MessageRequest;
import com.ellu.looper.fastapi.service.FastApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatConsumer {

  private final ObjectMapper objectMapper;
  private final FastApiService fastApiService;
  private final ChatProducer chatProducer;

  @KafkaListener(
      topics = "${kafka.topics.chatbot.user-input}",
      groupId = "${kafka.consumer.chat-group-id}")
  public void consumeUserMessage(ConsumerRecord<String, String> record) {
    try {
      String key = record.key(); // userId
      String value = record.value(); // JSON
      Long userId = Long.parseLong(key);

      MessageRequest message = objectMapper.readValue(value, MessageRequest.class);

      log.info("Received message from user {}: {}", userId, message.getMessage());

      fastApiService
          .streamChatResponse(message)
          .doOnNext(
              fullJson -> {
                log.info("FastAPI Response: {}", fullJson);
                chatProducer.sendChatbotResponse(userId, fullJson);
              })
          .doOnComplete(
              () -> {
                log.info("Chatbot response stream completed for user {}", userId);
              })
          .doOnError(
              error -> {
                log.error("Error processing chat message: {}", error.getMessage());
                chatProducer.sendResponseToken(userId, "Error: " + error.getMessage(), true);
              })
          .subscribe();

    } catch (Exception e) {
      log.error("Failed to process user message", e);
    }
  }
}
