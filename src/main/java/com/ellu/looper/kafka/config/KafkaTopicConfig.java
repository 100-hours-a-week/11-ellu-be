package com.ellu.looper.kafka.config;


import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {


  @Value("${kafka.topics.chatbot.user-input}")
  private String USER_INPUT_TOPIC;

  @Value("${kafka.topics.chatbot.response}")
  private String RESPONSE_TOPIC;

  @Bean
  public NewTopic chatTopic() {
    return new NewTopic(USER_INPUT_TOPIC, 3, (short) 1);
  }

  @Bean
  public NewTopic responseTopic() {
    return new NewTopic(RESPONSE_TOPIC, 3, (short) 1);
  }

}
