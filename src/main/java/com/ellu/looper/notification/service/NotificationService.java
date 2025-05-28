package com.ellu.looper.notification.service;

import com.ellu.looper.notification.dto.NotificationDto;
import com.ellu.looper.notification.entity.Notification;
import com.ellu.looper.notification.repository.NotificationRepository;
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
              .processed(n.getIsProcessed())
              .inviteStatus(n.getInviteStatus())
              .createdAt(n.getCreatedAt())
              .build();
        })
        .collect(Collectors.toList());
  }

  public String renderInvitationTemplate(String template, Notification notification) {
    return template
        .replace("{creator}", notification.getPayload().get("creator").toString())
        .replace("{project}", notification.getPayload().get("project").toString());
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
  public void markAsRead(Long notificationId, Long userId) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new IllegalArgumentException("알림 없음"));

    if (!notification.getReceiver().getId().equals(userId)) {
      throw new AccessDeniedException("본인의 알림만 읽음 처리할 수 있습니다.");
    }

    notification = notification.toBuilder().isProcessed(true).updatedAt(LocalDateTime.now())
        .build();
    notificationRepository.save(notification);
  }
}
