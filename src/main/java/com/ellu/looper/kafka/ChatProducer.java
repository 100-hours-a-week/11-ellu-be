package com.ellu.looper.kafka;

import com.ellu.looper.chat.dto.MessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatProducer {
  private final ObjectMapper objectMapper;
  private final KafkaTemplate<String, String> kafkaTemplate;

  @Value("${kafka.topics.chatbot.user-input}")
  private String USER_INPUT_TOPIC;

  @Value("${kafka.topics.chatbot.response}")
  private String RESPONSE_TOPIC;

  public void sendUserMessage(Long userId, MessageRequest message) {
    try {
      String payload = objectMapper.writeValueAsString(message);
      log.info("Publishing message from user {}: {}", userId, message.getMessage());
      kafkaTemplate.send(USER_INPUT_TOPIC, userId.toString(), payload);
    } catch (Exception e) {
      log.error("Failed to serialize MessageRequest", e);
      throw new RuntimeException("Serialization failed", e);
    }
  }

  public void sendResponseToken(Long userId, String token, boolean done) {
    try {
      ChatResponseToken response = new ChatResponseToken(token, done);
      String payload = objectMapper.writeValueAsString(response);
      kafkaTemplate.send(RESPONSE_TOPIC, userId.toString(), payload);
    } catch (Exception e) {
      log.error("Failed to serialize ChatResponseToken", e);
      throw new RuntimeException("Serialization failed", e);
    }
  }

  public record ChatResponseToken(String token, boolean done) {}
}
