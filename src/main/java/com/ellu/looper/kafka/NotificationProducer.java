package com.ellu.looper.kafka;

import com.ellu.looper.kafka.dto.NotificationMessage;
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
public class NotificationProducer {
  private static final Logger log =
      LoggerFactory.getLogger(NotificationProducer.class.getSimpleName());
  private final ObjectMapper objectMapper;
  private KafkaProducer<String, String> producer;

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @PostConstruct
  public void initialize() {
    // Create producer properties
    Properties properties = new Properties();
    properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.setProperty(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.setProperty(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");

    // Create producer
    producer = new KafkaProducer<>(properties);
    log.info("Kafka Producer initialized");
  }

  public void sendNotification(NotificationMessage event) {
    try {
      for (Long userId : event.getReceiverId()) {
        String key = userId.toString();
        String value = objectMapper.writeValueAsString(event);

        // Create producer record
        ProducerRecord<String, String> producerRecord =
            new ProducerRecord<>("notification", key, value);

        // Send data asynchronously with callback
        producer.send(
            producerRecord,
            new Callback() {
              @Override
              public void onCompletion(RecordMetadata metadata, Exception e) {
                if (e == null) {
                  log.info(
                      "Successfully sent notification to user {} \n"
                          + "Topic: {}\n"
                          + "Partition: {}\n"
                          + "Offset: {}\n"
                          + "Timestamp: {}",
                      userId,
                      metadata.topic(),
                      metadata.partition(),
                      metadata.offset(),
                      metadata.timestamp());
                } else {
                  log.error("Error while sending notification to user {}", userId, e);
                }
              }
            });
      }

    } catch (JsonProcessingException e) {
      log.error("Failed to serialize NotificationMessage", e);
      throw new RuntimeException("Failed to serialize NotificationMessage", e);
    }
  }

  public void shutdown() {
    if (producer != null) {
      producer.flush();
      producer.close();
      log.info("Producer has been closed");
    }
  }
}
