package com.ellu.looper.notification.service;

import com.ellu.looper.commons.enums.InviteStatus;
import com.ellu.looper.commons.enums.NotificationType;
import com.ellu.looper.commons.enums.Role;
import com.ellu.looper.kafka.NotificationProducer;
import com.ellu.looper.kafka.dto.NotificationMessage;
import com.ellu.looper.notification.dto.NotificationDto;
import com.ellu.looper.notification.entity.Notification;
import com.ellu.looper.notification.entity.NotificationTemplate;
import com.ellu.looper.notification.repository.NotificationRepository;
import com.ellu.looper.notification.repository.NotificationTemplateRepository;
import com.ellu.looper.project.dto.AddedMember;
import com.ellu.looper.project.entity.Project;
import com.ellu.looper.project.entity.ProjectMember;
import com.ellu.looper.project.repository.ProjectMemberRepository;
import com.ellu.looper.user.entity.User;
import com.ellu.looper.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private final UserRepository userRepository;

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

  public List<NotificationDto> getNotifications(Long userId) {
    List<Notification> notifications =
        notificationRepository.findByReceiverIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);

    return notifications.stream()
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

              return NotificationDto.builder()
                  .id(n.getId())
                  .senderNickname(n.getSender().getNickname())
                  .message(message)
                  .inviteStatus(n.getInviteStatus())
                  .createdAt(n.getCreatedAt())
                  .build();
            })
        .collect(Collectors.toList());
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
    User creator =
        userRepository
            .findById(creatorId)
            .orElseThrow(() -> new IllegalArgumentException("Project creator not found"));

    // Notification 생성
    NotificationTemplate inviteTemplate =
        notificationTemplateRepository
            .findByType(type)
            .orElseThrow(() -> new IllegalArgumentException("프로젝트 알림 템플릿 없음"));

    Map<String, Object> payload = new HashMap<>();
    payload.put("project", project.getTitle());

    for (ProjectMember member : toRemove) {
      User receiver =
          userRepository
              .findById(member.getUser().getId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Project notification receiver with id "
                              + member.getUser().getId()
                              + " not found"));

      Notification notification =
          Notification.builder()
              .sender(creator)
              .receiver(receiver)
              .project(project)
              .template(inviteTemplate)
              .payload(payload)
              .createdAt(LocalDateTime.now())
              .build();
      notificationRepository.save(notification);

      // Kafka를 통해 알림 메시지 전송
      NotificationMessage message =
          new NotificationMessage(
              type.toString(),
              notification.getId(),
              project.getId(),
              creator.getId(),
              List.of(member.getUser().getId()),
              renderProjectTemplate(inviteTemplate.getTemplate(), notification));

      log.info("TRYING TO SEND KAFKA MESSAGE: {}", message.getMessage());
      notificationProducer.sendNotification(message);
    }
  }

  public void sendInvitationNotification(
      List<User> addedUsers, User creator, Project project, List<AddedMember> addedMemberRequests) {
    // Notification 생성
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
      Notification notification =
          Notification.builder()
              .sender(creator)
              .receiver(user)
              .project(project)
              .template(inviteTemplate)
              .payload(payload)
              .inviteStatus(String.valueOf(InviteStatus.PENDING))
              .createdAt(LocalDateTime.now())
              .build();
      notificationRepository.save(notification);

      // Kafka를 통해 알림 메시지 전송
      NotificationMessage message =
          new NotificationMessage(
              NotificationType.PROJECT_INVITED.toString(),
              notification.getId(),
              project.getId(),
              creator.getId(),
              List.of(user.getId()),
              renderInvitationTemplate(inviteTemplate.getTemplate(), notification));

      log.info("TRYING TO SEND KAFKA MESSAGE: {}", message.getMessage());
      notificationProducer.sendNotification(message);
    }
  }

  @Transactional
  public NotificationDto respondToInvitation(Long notificationId, Long userId, String status) {
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
    if (status.equalsIgnoreCase(InviteStatus.ACCEPTED.toString())) {
      boolean alreadyMember =
          projectMemberRepository.existsByProjectIdAndUserIdAndDeletedAtIsNull(
              notification.getProject().getId(), userId);
      if (!alreadyMember) {
        ProjectMember member =
            ProjectMember.builder()
                .project(notification.getProject())
                .user(notification.getReceiver())
                .role(Role.PARTICIPANT)
                .position(notification.getPayload().get("position").toString())
                .build();
        projectMemberRepository.save(member);
        sendInvitationResponseNotification(
            notification.getReceiver(), notification.getProject(), "수락");
      }
    } else if (status.equalsIgnoreCase(InviteStatus.REJECTED.name())) {
      sendInvitationResponseNotification(
          notification.getReceiver(), notification.getProject(), "거부");
    }

    String message =
        renderInvitationTemplate(notification.getTemplate().getTemplate(), notification);
    return new NotificationDto(
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
    Notification notification =
        Notification.builder()
            .sender(sender)
            .receiver(projectCreator)
            .project(project)
            .template(inviteResponseTemplate)
            .payload(payload)
            .createdAt(LocalDateTime.now())
            .build();
    notificationRepository.save(notification);

    // Kafka를 통해 알림 메시지 전송
    NotificationMessage message =
        new NotificationMessage(
            NotificationType.INVITATION_PROCESSED.toString(),
            notification.getId(),
            project.getId(),
            sender.getId(),
            List.of(projectCreator.getId()),
            renderInvitationResponseTemplate(inviteResponseTemplate.getTemplate(), notification));

    log.info("TRYING TO SEND KAFKA MESSAGE: {}", message.getMessage());
    notificationProducer.sendNotification(message);
  }
}
