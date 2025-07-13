package com.ellu.looper.kafka;

import static com.ellu.looper.commons.util.CacheService.addJitter;

import com.ellu.looper.commons.enums.Role;
import com.ellu.looper.commons.util.CacheService;
import com.ellu.looper.fastapi.service.FastApiService;
import com.ellu.looper.kafka.dto.NotificationMessage;
import com.ellu.looper.notification.dto.NotificationDto;
import com.ellu.looper.notification.entity.Notification;
import com.ellu.looper.notification.entity.NotificationTemplate;
import com.ellu.looper.notification.repository.NotificationRepository;
import com.ellu.looper.notification.repository.NotificationTemplateRepository;
import com.ellu.looper.notification.service.NotificationService;
import com.ellu.looper.project.dto.CreatorExcludedProjectResponse;
import com.ellu.looper.project.dto.ProjectResponse;
import com.ellu.looper.project.entity.Project;
import com.ellu.looper.project.entity.ProjectMember;
import com.ellu.looper.project.repository.ProjectMemberRepository;
import com.ellu.looper.project.repository.ProjectRepository;
import com.ellu.looper.project.service.ProjectService;
import com.ellu.looper.schedule.entity.Assignee;
import com.ellu.looper.schedule.entity.ProjectSchedule;
import com.ellu.looper.schedule.repository.AssigneeRepository;
import com.ellu.looper.schedule.repository.ProjectScheduleRepository;
import com.ellu.looper.sse.service.NotificationSseService;
import com.ellu.looper.user.entity.User;
import com.ellu.looper.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class NotificationConsumer implements Runnable {

  private static final Logger log =
      LoggerFactory.getLogger(NotificationConsumer.class.getSimpleName());
  private final ObjectMapper objectMapper;
  private final NotificationSseService sseService;
  private KafkaConsumer<String, String> consumer;
  private volatile boolean running = true;

  private final NotificationRepository notificationRepository;
  private final NotificationService notificationService;
  private final UserRepository userRepository;
  private final ProjectRepository projectRepository;
  private final ProjectService projectService;
  private final ProjectMemberRepository projectMemberRepository;
  private final NotificationTemplateRepository notificationTemplateRepository;
  private final ProjectScheduleRepository projectScheduleRepository;
  private final AssigneeRepository assigneeRepository;
  private final FastApiService fastApiService;
  private final RedisTemplate<String, Object> redisTemplate;
  private final CacheService cacheService;

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${kafka.topics.notification}")
  public String NOTIFICATION_TOPIC;

  @Value("${cache.notification.ttl-seconds}")
  private long NOTIFICATION_CACHE_TTL_SECONDS;

  @Value("${cache.notification.user-key-prefix}")
  private String NOTIFICATION_CACHE_KEY_PREFIX;

  @Value("${cache.project.ttl-seconds}")
  private long PROJECT_CACHE_TTL_SECONDS;

  @Value("${cache.project.detail-key-prefix}")
  private String PROJECT_DETAIL_CACHE_KEY_PREFIX;

  @Value("${cache.project.list-key-prefix}")
  private String PROJECT_LIST_CACHE_KEY_PREFIX;

  @Value("${kafka.consumer.notification-group-id}")
  private String NOTIFICATION_GROUP_ID;

  @PostConstruct
  public void init() {
    log.info("NotificationConsumer init() called");
    this.start();
  }

  public void start() {
    // Create consumer properties
    Properties properties = new Properties();
    properties.setProperty("bootstrap.servers", bootstrapServers);
    properties.setProperty("key.deserializer", StringDeserializer.class.getName());
    properties.setProperty("value.deserializer", StringDeserializer.class.getName());
    properties.setProperty("group.id", NOTIFICATION_GROUP_ID);
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
              .findById(userId)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Notification receiver with id " + userId + " not found"));

      Project project =
          projectRepository
              .findById(event.getProjectId())
              .orElseThrow(
                  () ->
                      new EntityNotFoundException(
                          "Project with id " + event.getProjectId() + "not found"));

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
              .inviteStatus(event.getInviteStatus())
              .template(notificationTemplate)
              .payload(event.getPayload())
              .createdAt(LocalDateTime.now())
              .build();

      // DB 저장
      Notification saved = notificationRepository.save(notification);

      // Redis에 알림 저장 (write-through cache)
      String cacheKey = NOTIFICATION_CACHE_KEY_PREFIX + userId;
      List<Notification> notifications =
          notificationRepository.findByReceiverIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);

      List<NotificationDto> dtoList = notificationService.toDtoList(notifications);
      long notificationTtlWithJitter = addJitter(NOTIFICATION_CACHE_TTL_SECONDS, 0.1);
      redisTemplate
          .opsForValue()
          .set(cacheKey, dtoList, notificationTtlWithJitter, TimeUnit.MINUTES);

      // 초대 수락 알림 시 DB와 캐시 업데이트 (write-through)
      if (event.getType().equals("INVITATION_PROCESSED")
          && event.getPayload().get("status").equals("수락")) {
        Notification originalNotification =
            notificationRepository
                .findByIdAndDeletedAtIsNull(event.getNotificationId())
                .orElseThrow(
                    () ->
                        new EntityNotFoundException(
                            "Notification with id " + event.getNotificationId() + "not found"));

        // 중복 멤버 추가 방지
        boolean memberExists = projectMemberRepository
            .existsByProjectAndUserAndDeletedAtIsNull(project, originalNotification.getReceiver());
        
        if (!memberExists) {
          ProjectMember member =
              ProjectMember.builder()
                  .project(project)
                  .user(originalNotification.getReceiver())
                  .role(Role.PARTICIPANT)
                  .position(originalNotification.getPayload().get("position").toString())
                  .build();
          ProjectMember savedMember = projectMemberRepository.save(member);
        }

        // Redis에 프로젝트 멤버들의 프로젝트 리스트 업데이트
        List<ProjectMember> projectMembers =
            projectMemberRepository.findByProjectAndDeletedAtIsNull(project);
        projectMembers.forEach(
            pm -> {
              List<ProjectResponse> projectListDto =
                  projectService.getProjectListResponses(
                      originalNotification.getReceiver().getId());

              String projectMemberCacheKey = PROJECT_LIST_CACHE_KEY_PREFIX + pm.getUser().getId();
              cacheService.setProjectCache(
                  projectMemberCacheKey, projectListDto, PROJECT_CACHE_TTL_SECONDS);
            });

        // Redis에 해당 프로젝트 정보 업데이트
        CreatorExcludedProjectResponse projectDto =
            projectService.getCreatorExcludedProjectResponse(project.getMember().getId(), project);

        String projectCacheKey = PROJECT_DETAIL_CACHE_KEY_PREFIX + project.getId();
        cacheService.setProjectCache(projectCacheKey, projectDto, PROJECT_CACHE_TTL_SECONDS);

      } else if (event.getType().equals("PROJECT_EXPELLED")) {
        // Redis에 프로젝트 멤버들의 프로젝트 리스트 업데이트
        List<ProjectMember> projectMembers =
            projectMemberRepository.findByProjectAndDeletedAtIsNull(project);
        projectMembers.forEach(
            pm -> {
              List<ProjectResponse> projectListDto =
                  projectService.getProjectListResponses(notification.getSender().getId());

              String projectMemberCacheKey = PROJECT_LIST_CACHE_KEY_PREFIX + pm.getUser().getId();
              cacheService.setProjectCache(
                  projectMemberCacheKey, projectListDto, PROJECT_CACHE_TTL_SECONDS);
            });

        // Redis에 해당 프로젝트 정보 업데이트
        CreatorExcludedProjectResponse projectDto =
            projectService.getCreatorExcludedProjectResponse(project.getMember().getId(), project);
        String projectCacheKey = PROJECT_DETAIL_CACHE_KEY_PREFIX + project.getId();

        cacheService.setProjectCache(projectCacheKey, projectDto, PROJECT_CACHE_TTL_SECONDS);

      } else if (event.getType().equals("PROJECT_DELETED")) {
        Project deletedProject = project.toBuilder().deletedAt(LocalDateTime.now()).build();
        projectRepository.save(deletedProject);

        // 캐시 무효화
        redisTemplate.delete(PROJECT_DETAIL_CACHE_KEY_PREFIX + event.getProjectId());

        // 프로젝트 스케줄 assignees 삭제
        List<Assignee> assignees =
            assigneeRepository.findByProjectIdThroughScheduleAndDeletedAtIsNull(project.getId());
        for (Assignee assignee : assignees) {
          assignee.softDelete();
        }
        assigneeRepository.saveAll(assignees);

        // 프로젝트의 스케줄 삭제
        List<ProjectSchedule> schedules =
            projectScheduleRepository.findByProjectAndDeletedAtIsNull(project);
        for (ProjectSchedule schedule : schedules) {
          schedule.softDelete();
        }
        projectScheduleRepository.saveAll(schedules);

        // 프로젝트 멤버 삭제
        List<ProjectMember> members =
            projectMemberRepository.findByProjectAndDeletedAtIsNull(project);
        for (ProjectMember member : members) {
          member.setDeletedAt(LocalDateTime.now());
          // 캐시 무효화
          redisTemplate.delete(PROJECT_LIST_CACHE_KEY_PREFIX + member.getUser().getId());
        }
        projectMemberRepository.saveAll(members);

        // send wiki deletion request to FastApi
        fastApiService.deleteWiki(project.getId());
      }

      // SSE 구독 중인 유저에게 전송
      sseService.sendNotification(userId, event.toBuilder().notificationId(saved.getId()).build());
    }
  }
}
