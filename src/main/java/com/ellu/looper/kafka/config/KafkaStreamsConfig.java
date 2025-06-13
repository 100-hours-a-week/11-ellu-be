package com.ellu.looper.kafka.config;

import static org.apache.kafka.streams.StreamsConfig.*;

import com.ellu.looper.kafka.ChatProducer;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${spring.kafka.streams.application-id}")
  private String applicationId;

  @Value("${kafka.topics.chatbot.response}")
  private String responseTopic;

  @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
  public KafkaStreamsConfiguration kStreamsConfig() {
    Map<String, Object> props = new HashMap<>();
    props.put(APPLICATION_ID_CONFIG, applicationId);
    props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
    props.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class.getName());
    props.put(PROCESSING_GUARANTEE_CONFIG, EXACTLY_ONCE_V2);

    return new KafkaStreamsConfiguration(props);
  }

  @Bean
  public KStream<String, ChatProducer.ChatResponseToken> chatResponseStream(
      StreamsBuilder streamsBuilder) {
    return streamsBuilder.stream(
        responseTopic,
        Consumed.with(Serdes.String(), new JsonSerde<>(ChatProducer.ChatResponseToken.class)));
  }
}
