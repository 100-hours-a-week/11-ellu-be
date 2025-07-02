package com.ellu.looper.kafka;

import com.ellu.looper.kafka.dto.NotificationMessage;
import com.ellu.looper.notification.entity.Notification;
import com.ellu.looper.notification.entity.NotificationTemplate;
import com.ellu.looper.notification.repository.NotificationRepository;
import com.ellu.looper.notification.repository.NotificationTemplateRepository;
import com.ellu.looper.project.entity.Project;
import com.ellu.looper.project.repository.ProjectRepository;
import com.ellu.looper.sse.service.SseService;
import com.ellu.looper.user.entity.User;
import com.ellu.looper.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.LocalDateTime;
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

  private static final Logger log =
      LoggerFactory.getLogger(NotificationConsumer.class.getSimpleName());
  private final ObjectMapper objectMapper;
  private final SseService sseEmitterService;
  private KafkaConsumer<String, String> consumer;
  private volatile boolean running = true;

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final ProjectRepository projectRepository;
  private final NotificationTemplateRepository notificationTemplateRepository;

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${kafka.topics.notification}")
  public String NOTIFICATION_TOPIC;

  @PostConstruct
  public void init() {
    log.info("NotificationConsumer init() called");
    this.start();
  }

  public void start() {
    String groupId = "notification-service-group";

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

    // Start consuming in a new thread
    new Thread(this).start();
  }

  @Override
  public void run() {
    try {
      // Subscribe to the topic
      consumer.subscribe(Arrays.asList(NOTIFICATION_TOPIC));

      // Poll for data
      while (running) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

        for (ConsumerRecord<String, String> record : records) {
          try {
            NotificationMessage event =
                objectMapper.readValue(record.value(), NotificationMessage.class);
            processNotification(event);
            log.info(
                "Processed notification for partition: {}, offset: {}",
                record.partition(),
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
    for (Long userId : event.getReceiverId()) {
      User sender =
          userRepository
              .findById(event.getSenderId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Notification sender with id " + event.getSenderId() + "not found"));

      User receiver =
          userRepository
              .findById(event.getReceiverId().getFirst())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Notification receiver with id "
                              + event.getReceiverId().getFirst()
                              + " not found"));

      Project project =
          projectRepository
              .findById(event.getProjectId())
              .orElseThrow(() -> new EntityNotFoundException("Project Not found"));

      NotificationTemplate notificationTemplate =
          notificationTemplateRepository
              .findById(event.getTemplateId())
              .orElseThrow(
                  () ->
                      new EntityNotFoundException(
                          "Notification template is not stored in database."));

      Notification notification =
          Notification.builder()
              .sender(sender)
              .receiver(receiver)
              .project(project)
              .template(notificationTemplate)
              .payload(event.getPayload())
              .createdAt(LocalDateTime.now())
              .build();

      // DB 저장
      Notification saved = notificationRepository.save(notification);

      // Redis 저장

      // SSE 구독 중인 유저에게 전송
      sseEmitterService.sendNotification(
          userId, event.toBuilder().notificationId(saved.getId()).build());
    }
  }
}
