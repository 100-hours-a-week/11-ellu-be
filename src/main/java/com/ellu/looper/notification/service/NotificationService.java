package com.ellu.looper.notification.service;

import com.ellu.looper.commons.enums.InviteStatus;
import com.ellu.looper.commons.enums.Role;
import com.ellu.looper.notification.dto.NotificationDto;
import com.ellu.looper.notification.entity.Notification;
import com.ellu.looper.notification.repository.NotificationRepository;
import com.ellu.looper.project.entity.ProjectMember;
import com.ellu.looper.project.repository.ProjectMemberRepository;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
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

  public List<NotificationDto> getNotifications(Long userId) {
    List<Notification> notifications = notificationRepository.findByReceiverIdAndDeletedAtIsNullOrderByCreatedAtDesc(
        userId);

    return notifications.stream()
        .map(n -> {
          String message;
          Long templateId = n.getTemplate().getId();

          if (templateId == 1L) {
            message = renderInvitationTemplate(n.getTemplate().getTemplate(), n);
          } else if (templateId == 2L || templateId == 3L) {
            message = renderProjectTemplate(n.getTemplate().getTemplate(), n);
          } else if (templateId == 4L || templateId == 5L || templateId == 6L) {
            message = renderScheduleTemplate(n.getTemplate().getTemplate(), n);
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

  public String renderInvitationTemplate(String template, Notification notification) {
    return template
        .replace("{creator}", notification.getPayload().get("creator").toString())
        .replace("{project}", notification.getPayload().get("project").toString())
        .replace("{position}", notification.getPayload().get("position").toString());
  }

  public String renderProjectTemplate(String template, Notification notification) {
    return template
        .replace("{project}", notification.getPayload().get("project").toString());
  }

  public String renderScheduleTemplate(String template, Notification notification) {
    return template
        .replace("{schedule}", notification.getPayload().get("schedule").toString())
        .replace("{project}", notification.getPayload().get("project").toString());
  }

  @Transactional
  public NotificationDto respondToInvitation(Long notificationId, Long userId, String status) {
    Notification notification = notificationRepository.findByIdAndDeletedAtIsNull(notificationId)
        .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

    if (!notification.getReceiver().getId().equals(userId)) {
      throw new AccessDeniedException("Unauthorized to process this notification.");
    }

    if (!status.equalsIgnoreCase(InviteStatus.ACCEPTED.toString()) && !status.equalsIgnoreCase(InviteStatus.REJECTED.toString())) {
      throw new IllegalArgumentException("Status must be 'ACCEPTED' or 'REJECTED'.");
    }

    notification = notification.toBuilder()
        .inviteStatus(status.toUpperCase())
        .updatedAt(LocalDateTime.now())
        .build();

    notificationRepository.save(notification);

    if (status.equalsIgnoreCase(InviteStatus.ACCEPTED.toString())) {
      boolean alreadyMember = projectMemberRepository.existsByProjectIdAndUserId(
          notification.getProject().getId(), userId);
      if (!alreadyMember) {
        ProjectMember member = ProjectMember.builder()
            .project(notification.getProject())
            .user(notification.getReceiver())
            .role(Role.PARTICIPANT)
            .position(notification.getPayload().get("position").toString())
            .build();
        projectMemberRepository.save(member);
      }
    }

    String message = renderInvitationTemplate(notification.getTemplate().getTemplate(), notification);
    return new NotificationDto(notificationId, notification.getSender().getNickname(), message,  notification.getInviteStatus(), notification.getCreatedAt());
  }

}
