package com.ellu.looper.kafka;

import com.ellu.looper.chat.dto.MessageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatProducer {
  private final KafkaTemplate<String, Object> kafkaTemplate;

  @Value("${kafka.topics.chatbot.user-input}")
  private String USER_INPUT_TOPIC;

  @Value("${kafka.topics.chatbot.response}")
  private String RESPONSE_TOPIC;

  public void sendUserMessage(Long userId, MessageRequest message) {
    log.info("Publishing message from user {}: {}", userId, message.getMessage());
    kafkaTemplate.send(USER_INPUT_TOPIC, userId.toString(), message);
  }

  public void sendResponseToken(Long userId, String token, boolean done) {
    kafkaTemplate.send(RESPONSE_TOPIC, userId.toString(), new ChatResponseToken(token, done));
  }

  public record ChatResponseToken(String token, boolean done) {}
}
