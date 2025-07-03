package com.ellu.looper.notification.service;

import com.ellu.looper.commons.enums.InviteStatus;
import com.ellu.looper.commons.enums.NotificationType;
import com.ellu.looper.commons.enums.Role;
import com.ellu.looper.kafka.NotificationProducer;
import com.ellu.looper.kafka.dto.NotificationMessage;
import com.ellu.looper.notification.dto.NotificationDto;
import com.ellu.looper.notification.dto.NotificationResponse;
import com.ellu.looper.notification.entity.Notification;
import com.ellu.looper.notification.entity.NotificationTemplate;
import com.ellu.looper.notification.repository.NotificationRepository;
import com.ellu.looper.notification.repository.NotificationTemplateRepository;
import com.ellu.looper.project.dto.AddedMember;
import com.ellu.looper.project.dto.CreatorExcludedProjectResponse;
import com.ellu.looper.project.dto.ProjectResponse;
import com.ellu.looper.project.entity.Project;
import com.ellu.looper.project.entity.ProjectMember;
import com.ellu.looper.project.repository.ProjectMemberRepository;
import com.ellu.looper.project.service.ProjectService;
import com.ellu.looper.user.entity.User;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final NotificationTemplateRepository notificationTemplateRepository;
  private final NotificationProducer notificationProducer;
  private final ProjectService projectService;
  private final RedisTemplate<String, Object> redisTemplate;

  @Value("${cache.notification.user-ttl-minutes}")
  private long NOTIFICATION_CACHE_TTL_MINUTES;

  @Value("${cache.project.ttl-hours}")
  private long PROJECT_CACHE_TTL_HOURS;

  @Value("${cache.notification.user-key-prefix}")
  private String NOTIFICATION_CACHE_KEY_PREFIX;

  @Value("${cache.project.detail-key-prefix}")
  private String PROJECT_DETAIL_CACHE_KEY_PREFIX;

  @Value("${cache.project.list-key-prefix}")
  private String PROJECT_LIST_CACHE_KEY_PREFIX;

  @Transactional
  public void softDeleteOldNotifications() {
    LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
    List<Notification> oldNotifications =
        notificationRepository.findByCreatedAtBeforeAndDeletedAtIsNull(oneWeekAgo);

    for (Notification notification : oldNotifications) {
      notification.softDelete(LocalDateTime.now());
    }

    notificationRepository.saveAll(oldNotifications);

    // TODO: 배치 처리 실패 처리 로직 추가, shedlock 처리 추가
  }

  public List<NotificationResponse> getNotifications(Long userId) {
    String cacheKey = NOTIFICATION_CACHE_KEY_PREFIX + userId;
    List<NotificationResponse> cached =
        (List<NotificationResponse>) redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) {
      log.info("Cache hit for notifications: {}", userId);
      return cached;
    }

    log.info("Cache miss for notifications: {}", userId);

    List<Notification> notifications =
        notificationRepository.findByReceiverIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);

    List<NotificationResponse> notificationDtoList =
        notifications.stream()
            .map(
                n -> {
                  String message;
                  NotificationType type = n.getTemplate().getType();

                  if (type.equals(NotificationType.PROJECT_INVITED)) {
                    message = renderInvitationTemplate(n.getTemplate().getTemplate(), n);
                  } else if (type.equals(NotificationType.PROJECT_DELETED)
                      || type.equals(NotificationType.PROJECT_EXPELLED)
                      || type.equals(NotificationType.PROJECT_WIKI_READY)) {
                    message = renderProjectTemplate(n.getTemplate().getTemplate(), n);
                  } else if (type.equals(NotificationType.SCHEDULE_CREATED)
                      || type.equals(NotificationType.SCHEDULE_UPDATED)
                      || type.equals(NotificationType.SCHEDULE_DELETED)) {
                    message = renderScheduleTemplate(n.getTemplate().getTemplate(), n);
                  } else if (type.equals(NotificationType.INVITATION_PROCESSED)) {
                    message = renderInvitationResponseTemplate(n.getTemplate().getTemplate(), n);
                  } else {
                    message = "";
                  }

                  return NotificationResponse.builder()
                      .id(n.getId())
                      .senderNickname(n.getSender().getNickname())
                      .message(message)
                      .inviteStatus(n.getInviteStatus())
                      .createdAt(n.getCreatedAt())
                      .build();
                })
            .collect(Collectors.toList());

    redisTemplate
        .opsForValue()
        .set(cacheKey, notificationDtoList, NOTIFICATION_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    return notificationDtoList;
  }

  private String renderInvitationResponseTemplate(String template, Notification notification) {
    return template
        .replace("{receiver}", notification.getPayload().get("receiver").toString())
        .replace("{project}", notification.getPayload().get("project").toString())
        .replace("{status}", notification.getPayload().get("status").toString());
  }

  public String renderInvitationTemplate(String template, Notification notification) {
    return template
        .replace("{creator}", notification.getPayload().get("creator").toString())
        .replace("{project}", notification.getPayload().get("project").toString())
        .replace("{position}", notification.getPayload().get("position").toString());
  }

  public String renderProjectTemplate(String template, Notification notification) {
    return template.replace("{project}", notification.getPayload().get("project").toString());
  }

  public String renderScheduleTemplate(String template, Notification notification) {
    return template
        .replace("{schedule}", notification.getPayload().get("schedule").toString())
        .replace("{project}", notification.getPayload().get("project").toString());
  }

  @Transactional
  public void sendProjectNotification(
      NotificationType type, List<ProjectMember> toRemove, Long creatorId, Project project) {
    NotificationTemplate inviteTemplate =
        notificationTemplateRepository
            .findByType(type)
            .orElseThrow(() -> new IllegalArgumentException("프로젝트 알림 템플릿 없음"));

    Map<String, Object> payload = new HashMap<>();
    payload.put("project", project.getTitle());

    for (ProjectMember member : toRemove) {
      Notification notification = Notification.builder().payload(payload).build();

      // Kafka를 통해 알림 메시지 전송
      NotificationMessage message =
          new NotificationMessage(
              type.toString(),
              notification.getId(),
              project.getId(),
              creatorId,
              List.of(member.getUser().getId()),
              renderProjectTemplate(inviteTemplate.getTemplate(), notification),
              inviteTemplate.getId(),
              payload,
              null);

      notificationProducer.sendNotification(message);
    }
  }

  public void sendInvitationNotification(
      List<User> addedUsers, User creator, Project project, List<AddedMember> addedMemberRequests) {
    NotificationTemplate inviteTemplate =
        notificationTemplateRepository
            .findByType(NotificationType.PROJECT_INVITED)
            .orElseThrow(() -> new IllegalArgumentException("초대 알림 템플릿 없음"));
    Map<String, String> nicknameToPosition =
        addedMemberRequests.stream()
            .collect(Collectors.toMap(AddedMember::getNickname, AddedMember::getPosition));

    for (User user : addedUsers) {
      Map<String, Object> payload = new HashMap<>();
      payload.put("creator", creator.getNickname());
      payload.put("project", project.getTitle());
      payload.put("position", nicknameToPosition.get(user.getNickname()));
      Notification notification = Notification.builder().payload(payload).build();

      // Kafka를 통해 알림 메시지 전송
      NotificationMessage message =
          new NotificationMessage(
              NotificationType.PROJECT_INVITED.toString(),
              null,
              project.getId(),
              creator.getId(),
              List.of(user.getId()),
              renderInvitationTemplate(inviteTemplate.getTemplate(), notification),
              inviteTemplate.getId(),
              payload,
              InviteStatus.PENDING.name());

      notificationProducer.sendNotification(message);
    }
  }

  @Transactional
  public NotificationResponse respondToInvitation(Long notificationId, Long userId, String status) {
    Notification notification =
        notificationRepository
            .findByIdAndDeletedAtIsNull(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

    if (!notification.getReceiver().getId().equals(userId)) {
      throw new AccessDeniedException("Unauthorized to process this notification.");
    }

    if (!status.equalsIgnoreCase(InviteStatus.ACCEPTED.toString())
        && !status.equalsIgnoreCase(InviteStatus.REJECTED.toString())) {
      throw new IllegalArgumentException("Status must be 'ACCEPTED' or 'REJECTED'.");
    }

    notification =
        notification.toBuilder()
            .inviteStatus(status.toUpperCase())
            .updatedAt(LocalDateTime.now())
            .build();

    notificationRepository.save(notification);

    // Redis 저장 (write-through cache)
    String cacheKey = NOTIFICATION_CACHE_KEY_PREFIX + notification.getSender().getId();
    List<Notification> notifications =
        notificationRepository.findByReceiverIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            notification.getSender().getId());

    List<NotificationDto> dtoList = toDtoList(notifications);
    redisTemplate
        .opsForValue()
        .set(cacheKey, dtoList, NOTIFICATION_CACHE_TTL_MINUTES, TimeUnit.MINUTES);

    Project project = notification.getProject();

    // 초대 수락
    if (status.equalsIgnoreCase(InviteStatus.ACCEPTED.toString())) {
      // 프로젝트 멤버 추가
      boolean alreadyMember =
          projectMemberRepository.existsByProjectIdAndUserIdAndDeletedAtIsNull(
              project.getId(), userId);
      if (!alreadyMember) {
        ProjectMember member =
            ProjectMember.builder()
                .project(project)
                .user(notification.getReceiver())
                .role(Role.PARTICIPANT)
                .position(notification.getPayload().get("position").toString())
                .build();
        ProjectMember savedMember = projectMemberRepository.save(member);

        // Redis에 프로젝트 멤버들의 프로젝트 리스트 업데이트
        List<ProjectResponse> projectListDto =
            projectService.getProjectListResponses(notification.getReceiver().getId());
        String projectMemberCacheKey =
            PROJECT_LIST_CACHE_KEY_PREFIX + savedMember.getUser().getId();
        redisTemplate
            .opsForValue()
            .set(projectMemberCacheKey, projectListDto, PROJECT_CACHE_TTL_HOURS, TimeUnit.HOURS);

        // Redis에 해당 프로젝트 정보 업데이트
        CreatorExcludedProjectResponse projectDto =
            projectService.getCreatorExcludedProjectResponse(project.getMember().getId(), project);
        String projectCacheKey = PROJECT_DETAIL_CACHE_KEY_PREFIX + project.getId();
        redisTemplate
            .opsForValue()
            .set(projectCacheKey, projectDto, PROJECT_CACHE_TTL_HOURS, TimeUnit.HOURS);
        // 초대 처리 알림 전송
        sendInvitationResponseNotification(notification.getReceiver(), project, "수락");
      }
      // 초대 거부
    } else if (status.equalsIgnoreCase(InviteStatus.REJECTED.name())) {
      sendInvitationResponseNotification(notification.getReceiver(), project, "거부");
    }

    String message =
        renderInvitationTemplate(notification.getTemplate().getTemplate(), notification);
    return new NotificationResponse(
        notificationId,
        notification.getSender().getNickname(),
        message,
        notification.getInviteStatus(),
        notification.getCreatedAt());
  }

  private void sendInvitationResponseNotification(User sender, Project project, String status) {
    // Notification 생성
    NotificationTemplate inviteResponseTemplate =
        notificationTemplateRepository
            .findByType(NotificationType.INVITATION_PROCESSED)
            .orElseThrow(() -> new IllegalArgumentException("초대 수락/거부 알림 템플릿 없음"));

    User projectCreator = project.getMember();
    Map<String, Object> payload = new HashMap<>();
    payload.put("receiver", sender.getNickname()); // 초대 알림을 받은 사람, 초대 응답을 보내는 사람
    payload.put("status", status);
    payload.put("project", project.getTitle());
    Notification notification = Notification.builder().payload(payload).build();

    // Kafka를 통해 알림 메시지 전송
    NotificationMessage message =
        new NotificationMessage(
            NotificationType.INVITATION_PROCESSED.toString(),
            notification.getId(),
            project.getId(),
            sender.getId(),
            List.of(projectCreator.getId()),
            renderInvitationResponseTemplate(inviteResponseTemplate.getTemplate(), notification),
            inviteResponseTemplate.getId(),
            payload,
            status);

    log.info("TRYING TO SEND KAFKA MESSAGE: {}", message.getMessage());
    notificationProducer.sendNotification(message);
  }

  public NotificationDto toDto(Notification notification) {
    return NotificationDto.builder()
        .id(notification.getId())
        .senderId(notification.getSender() != null ? notification.getSender().getId() : null)
        .receiverId(notification.getReceiver() != null ? notification.getReceiver().getId() : null)
        .projectId(notification.getProject() != null ? notification.getProject().getId() : null)
        .templateId(notification.getTemplate() != null ? notification.getTemplate().getId() : null)
        .payload(notification.getPayload())
        .inviteStatus(notification.getInviteStatus())
        .createdAt(notification.getCreatedAt())
        .updatedAt(notification.getUpdatedAt())
        .build();
  }

  public List<NotificationDto> toDtoList(List<Notification> notifications) {
    return notifications.stream().map(this::toDto).collect(Collectors.toList());
  }
}
