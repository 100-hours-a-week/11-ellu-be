package com.ellu.looper.schedule.config;

import com.ellu.looper.schedule.dto.PreviewMessage;
import com.ellu.looper.schedule.service.PreviewHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class PreviewPubSubConfig {

  @Bean
  public RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory,
      MessageListenerAdapter previewListenerAdapter) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(previewListenerAdapter, new ChannelTopic("preview-complete"));
    return container;
  }

  @Bean
  public MessageListenerAdapter previewListenerAdapter(PreviewHolder previewHolder) {
    return new MessageListenerAdapter(new Object() {
      public void handleMessage(Object message) {
        if (message instanceof PreviewMessage preview) {
          previewHolder.complete(preview.getProjectId(), preview.getResponse());
        }
      }
    }, "handleMessage");
  }
}
