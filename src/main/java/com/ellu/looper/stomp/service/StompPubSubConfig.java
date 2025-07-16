package com.ellu.looper.stomp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
public class StompPubSubConfig {
  private final RedisConnectionFactory redisConnectionFactory;
  private final StompPubSubListener stompPubSubListener;

  @Bean
  public RedisMessageListenerContainer stompRedisMessageListenerContainer() {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(redisConnectionFactory);
    container.addMessageListener(stompPubSubListener, new ChannelTopic("stomp:events"));
    return container;
  }
}
