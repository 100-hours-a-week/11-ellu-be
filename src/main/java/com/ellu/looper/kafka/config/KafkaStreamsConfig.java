package com.ellu.looper.kafka.config;

import static org.apache.kafka.streams.StreamsConfig.*;

import com.ellu.looper.sse.service.ChatSseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
    props.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
    props.put(PROCESSING_GUARANTEE_CONFIG, EXACTLY_ONCE_V2);

    return new KafkaStreamsConfiguration(props);
  }

  @Bean
  public KStream<String, String> chatResponseStream(
      StreamsBuilder streamsBuilder, ChatSseService sseService) {
    KStream<String, String> stream =
        streamsBuilder.stream(responseTopic, Consumed.with(Serdes.String(), Serdes.String()));

    stream.foreach(
        (key, value) -> {
          String cleanedValue = value;

          if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
            // 첫 글자와 마지막 글자를 제거
            cleanedValue = value.substring(1, value.length() - 1);

            cleanedValue = cleanedValue.replace("\\\"", "\"");
          }
          log.info("Formatted Kafka Streams message: {}", cleanedValue);

          try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(cleanedValue);
            JsonNode messageNode = root.get("message");
            JsonNode dataNode = root.get("data");

            if (dataNode == null) {
              log.warn(
                  "Kafka message does not contain a 'data' field. Skipping message. Key: {}, Value: {}",
                  key,
                  value);
              return;
            }

            if (messageNode.asText().equals("chatbot_message")) { // 스케줄 생성 진행 상태를 알려주는 메시지
              String text = dataNode.get("text").asText();
              boolean done = dataNode.get("done").asBoolean();

              sseService.sendMessage(key, text, done);

            } else if (messageNode.asText().equals("task_response")) { // 스케줄 데이터가 포함된 메시지
              JsonNode taskTitleNode = dataNode.get("task_title");
              JsonNode categoryNode = dataNode.get("category");
              JsonNode detailNode = dataNode.get("detail");
              JsonNode doneNode = dataNode.get("done");

              String planTitle = taskTitleNode.asText();
              String category = categoryNode.asText();
              boolean done = doneNode.asBoolean();

              // detail 필드 내부 파싱
              String subtaskTitle = null;
              String startTime = null;
              String endTime = null;

              if (detailNode.has("subtasks") && detailNode.get("subtasks").isTextual()) {
                subtaskTitle = detailNode.get("subtasks").asText();
              }
              if (detailNode.has("start_time") && detailNode.get("start_time").isTextual()) {
                startTime = detailNode.get("start_time").asText();
              }
              if (detailNode.has("end_time") && detailNode.get("end_time").isTextual()) {
                endTime = detailNode.get("end_time").asText();
              }

              sseService.sendSchedulePreview(
                  key, planTitle, category, subtaskTitle, startTime, endTime, done);

            } else { // 일반적인 대화 메시지
              String token = dataNode.get("token").asText();
              boolean done = dataNode.get("done").asBoolean();

              sseService.sendMessage(key, token, done);
            }

          } catch (JsonProcessingException e) {
            log.error("Invalid JSON format in Kafka message: {}", value, e);
          } catch (NullPointerException e) {
            log.error("Missing expected fields in Kafka message: {}", value, e);
          } catch (Exception e) {
            log.error("Unexpected error while processing Kafka message: {}", value, e);
          }
        });

    return stream;
  }
}
