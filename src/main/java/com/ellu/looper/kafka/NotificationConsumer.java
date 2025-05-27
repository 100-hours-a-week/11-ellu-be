package com.ellu.looper.kafka;

import com.ellu.looper.kafka.dto.NotificationMessage;
import com.ellu.looper.sse.service.SseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class NotificationConsumer implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(
      NotificationConsumer.class.getSimpleName());
  private final ObjectMapper objectMapper;
  private final SseService sseEmitterService;
  private KafkaConsumer<String, String> consumer;
  private volatile boolean running = true;

  @Value("${kafka.bootstrap-servers}")
  private String bootstrapServers;

  @PostConstruct
  public void init() {
    log.info("NotificationConsumer init() called");
    this.start();
  }

  public void start() {
    String groupId = "notification-service-group";
    String topic = "notification";

    // Create consumer properties
    Properties properties = new Properties();
    properties.setProperty("bootstrap.servers", bootstrapServers);
    properties.setProperty("key.deserializer", StringDeserializer.class.getName());
    properties.setProperty("value.deserializer", StringDeserializer.class.getName());
    properties.setProperty("group.id", groupId);
    properties.setProperty("auto.offset.reset", "earliest");

    // Create consumer
    consumer = new KafkaConsumer<>(properties);

    // Get a reference to the current thread
    final Thread mainThread = Thread.currentThread();

    // Adding the shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        log.info("Detected a shutdown, exit by calling consumer.wakeup()");
        running = false;
        consumer.wakeup();
        try {
          mainThread.join();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    });

    // Start consuming in a new thread
    new Thread(this).start();
  }

  @Override
  public void run() {
    try {
      // Subscribe to the topic
      consumer.subscribe(Arrays.asList("notification"));

      // Poll for data
      while (running) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

        for (ConsumerRecord<String, String> record : records) {
          try {
            NotificationMessage event = objectMapper.readValue(record.value(),
                NotificationMessage.class);
            processNotification(event);
            log.info("Processed notification for partition: {}, offset: {}", record.partition(),
                record.offset());
          } catch (Exception e) {
            log.error("Error processing notification: {}", e.getMessage(), e);
          }
        }
      }
    } catch (WakeupException e) {
      log.info("Consumer is starting to shut down");
    } catch (Exception e) {
      log.error("Unexpected exception in the consumer", e);
    } finally {
      consumer.close();
      log.info("The consumer is now gracefully shut down");
    }
  }

  private void processNotification(NotificationMessage event) {
    for (Long userId : event.getTargetUserIds()) {
      // SSE 구독 중인 유저에게 전송
      sseEmitterService.sendNotification(userId, event);
    }
  }
}

