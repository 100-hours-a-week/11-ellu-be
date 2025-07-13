package com.ellu.looper.kafka;

import com.ellu.looper.kafka.dto.ScheduleEventMessage;
import com.ellu.looper.schedule.dto.ProjectScheduleResponse;
import com.ellu.looper.schedule.entity.ProjectSchedule;
import com.ellu.looper.schedule.repository.ProjectScheduleRepository;
import com.ellu.looper.schedule.service.ProjectScheduleService;
import com.ellu.looper.stomp.service.StompService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
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
public class ScheduleEventConsumer implements Runnable {
  private static final Logger log =
      LoggerFactory.getLogger(ScheduleEventConsumer.class.getSimpleName());
  private final ObjectMapper objectMapper;
  private KafkaConsumer<String, String> consumer;
  private volatile boolean running = true;

  private final ProjectScheduleService projectScheduleService;
  private final ProjectScheduleRepository projectScheduleRepository;
  private final StompService stompService;

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${kafka.topics.schedule}")
  public String SCHEDULE_TOPIC;

  @Value("${kafka.consumer.schedule-group-id}")
  private String SCHEDULE_GROUP_ID;

  @Value("${server.port}")
  private int serverPort;

  @Value("${server.ip}")
  private String podIp;

  private String podId;
  private String uniqueGroupId;

  @PostConstruct
  public void init() {
    this.podId = "POD-" + podIp + "-" + serverPort;
    this.uniqueGroupId = SCHEDULE_GROUP_ID + "-" + podId;
    log.info("ScheduleEventConsumer init() called with group ID: {}", uniqueGroupId);
    this.start();
  }

  public void start() {
    Properties properties = new Properties();
    properties.setProperty("bootstrap.servers", bootstrapServers);
    properties.setProperty("key.deserializer", StringDeserializer.class.getName());
    properties.setProperty("value.deserializer", StringDeserializer.class.getName());
    properties.setProperty("group.id", uniqueGroupId);
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
    log.info(
        "Processing schedule event: type={}, projectId={}, userId={}, scheduleId={}",
        event.getType(),
        event.getProjectId(),
        event.getUserId(),
        event.getScheduleId());

    switch (event.getType()) {
      case "SCHEDULE_CREATED":
        List<ProjectScheduleResponse> createdList =
            projectScheduleService.createSchedules(
                Long.valueOf(event.getProjectId()), event.getUserId(), event.getCreateRequest());
        for (ProjectScheduleResponse response : createdList) {
          event = event.toBuilder().schedule(projectScheduleService.toDto(response)).build();

          // WebSocket 브로드캐스트
          log.info("Broadcasting SCHEDULE_CREATED to /topic/{}", event.getProjectId());
          stompService.sendMessage("/topic/" + event.getProjectId(), event);
        }
        break;

      case "SCHEDULE_UPDATED":
        ProjectScheduleResponse projectScheduleResponse =
            projectScheduleService.updateSchedule(
                event.getScheduleId(), event.getUserId(), event.getUpdateRequest());

        event =
            event.toBuilder()
                .schedule(projectScheduleService.toDto(projectScheduleResponse))
                .build();

        // WebSocket 브로드캐스트
        log.info("Broadcasting SCHEDULE_UPDATED to /topic/{}", event.getProjectId());
        stompService.sendMessage("/topic/" + event.getProjectId(), event);
        break;

      case "SCHEDULE_TAKEN":
        projectScheduleService.takeSchedule(
            Long.valueOf(event.getProjectId()), event.getScheduleId(), event.getUserId());

        // 반영된 스케줄 정보 조회
        ProjectSchedule updatedSchedule =
            projectScheduleRepository
                .findWithDetailsById(event.getScheduleId())
                .orElseThrow(() -> new EntityNotFoundException("Project schedule not found"));

        event =
            event.toBuilder()
                .schedule(
                    projectScheduleService.toDto(
                        projectScheduleService.toResponse(updatedSchedule)))
                .build();

        // WebSocket 브로드캐스트
        log.info("Broadcasting SCHEDULE_TAKEN to /topic/{}", event.getProjectId());
        stompService.sendMessage("/topic/" + event.getProjectId(), event);
        break;

      case "SCHEDULE_DELETED":
        projectScheduleService.deleteSchedule(event.getScheduleId(), event.getUserId());

        // WebSocket 브로드캐스트
        log.info("Broadcasting SCHEDULE_DELETED to /topic/{}", event.getProjectId());
        stompService.sendMessage("/topic/" + event.getProjectId(), event);
        break;
    }
  }
}
