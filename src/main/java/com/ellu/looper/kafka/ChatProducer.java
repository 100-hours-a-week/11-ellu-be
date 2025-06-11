package com.ellu.looper.kafka;

import com.ellu.looper.chat.dto.MessageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatProducer {
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private static final String USER_INPUT_TOPIC = "chatbot-user-input";
  private static final String RESPONSE_TOPIC = "chatbot-response";

  public void sendUserMessage(Long userId, MessageRequest message) {
    log.info("Publishing message from user {}: {}", userId, message.getMessage());
    kafkaTemplate.send(USER_INPUT_TOPIC, userId.toString(), message);
  }

  public void sendResponseToken(Long userId, String token, boolean done) {
    kafkaTemplate.send(RESPONSE_TOPIC, userId.toString(), new ChatResponseToken(token, done));
  }

  public record ChatResponseToken(String token, boolean done) {}
}
