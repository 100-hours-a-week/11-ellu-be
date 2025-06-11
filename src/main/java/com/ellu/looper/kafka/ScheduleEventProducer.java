package com.ellu.looper.kafka;

import com.ellu.looper.kafka.dto.ScheduleEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ScheduleEventProducer {
  private static final Logger log =
      LoggerFactory.getLogger(ScheduleEventProducer.class.getSimpleName());
  private final ObjectMapper objectMapper;
  private KafkaProducer<String, String> producer;
  private static final String TOPIC = "schedule";

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @PostConstruct
  public void initialize() {
    Properties properties = new Properties();
    properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.setProperty(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.setProperty(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, "400");

    producer = new KafkaProducer<>(properties);
    log.info("Schedule Kafka Producer initialized");
  }

  public void sendScheduleEvent(ScheduleEventMessage event) {
    try {
      String key = event.getProjectId();
      String value = objectMapper.writeValueAsString(event);

      ProducerRecord<String, String> producerRecord = new ProducerRecord<>(TOPIC, key, value);

      producer.send(
          producerRecord,
          new Callback() {
            @Override
            public void onCompletion(RecordMetadata metadata, Exception e) {
              if (e == null) {
                log.info(
                    "Successfully sent schedule event for project {} \n"
                        + "Topic: {}\n"
                        + "Partition: {}\n"
                        + "Offset: {}\n"
                        + "Timestamp: {}",
                    event.getProjectId(),
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset(),
                    metadata.timestamp());
              } else {
                log.error(
                    "Error while sending schedule event for project {}", event.getProjectId(), e);
              }
            }
          });

      producer.flush();
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize ScheduleEventMessage", e);
      throw new RuntimeException("Failed to serialize ScheduleEventMessage", e);
    }
  }

  public void shutdown() {
    if (producer != null) {
      producer.flush();
      producer.close();
      log.info("Schedule Producer has been closed");
    }
  }
}
