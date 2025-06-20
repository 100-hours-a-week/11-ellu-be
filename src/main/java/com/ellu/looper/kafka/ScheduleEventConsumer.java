package com.ellu.looper.kafka;

import com.ellu.looper.kafka.dto.ScheduleEventMessage;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ScheduleEventConsumer implements Runnable {
  private static final Logger log =
      LoggerFactory.getLogger(ScheduleEventConsumer.class.getSimpleName());
  private final ObjectMapper objectMapper;
  private final SimpMessagingTemplate messagingTemplate;
  private KafkaConsumer<String, String> consumer;
  private volatile boolean running = true;

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${kafka.topics.schedule}")
  public String SCHEDULE_TOPIC;

  @PostConstruct
  public void init() {
    log.info("ScheduleEventConsumer init() called");
    this.start();
  }

  public void start() {
    String groupId = "schedule-service-group";

    Properties properties = new Properties();
    properties.setProperty("bootstrap.servers", bootstrapServers);
    properties.setProperty("key.deserializer", StringDeserializer.class.getName());
    properties.setProperty("value.deserializer", StringDeserializer.class.getName());
    properties.setProperty("group.id", groupId);
    properties.setProperty("auto.offset.reset", "earliest");

    consumer = new KafkaConsumer<>(properties);

    final Thread mainThread = Thread.currentThread();

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
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

    new Thread(this).start();
  }

  @Override
  public void run() {
    try {
      consumer.subscribe(Arrays.asList(SCHEDULE_TOPIC));

      while (running) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

        for (ConsumerRecord<String, String> record : records) {
          try {
            ScheduleEventMessage event =
                objectMapper.readValue(record.value(), ScheduleEventMessage.class);
            processScheduleEvent(event);
            log.info(
                "Processed schedule event for partition: {}, offset: {}",
                record.partition(),
                record.offset());
          } catch (Exception e) {
            log.error("Error processing schedule event: {}", e.getMessage(), e);
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

  private void processScheduleEvent(ScheduleEventMessage event) {
    messagingTemplate.convertAndSend("/topic/" + event.getProjectId(), event);
  }
}
