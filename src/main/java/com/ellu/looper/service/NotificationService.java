package com.ellu.looper.service;

import com.ellu.looper.dto.NotificationDto;
import com.ellu.looper.entity.Notification;
import com.ellu.looper.repository.NotificationRepository;
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
    List<Notification> notifications =
        notificationRepository.findByReceiverIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);

    return notifications.stream()
        .map(
            n ->
                NotificationDto.builder()
                    .id(n.getId())
                    .senderNickname(n.getSender().getNickname())
                    .message(renderTemplate(n.getTemplate().getTemplate(), n))
                    .processed(n.getIsProcessed())
                    .createdAt(n.getCreatedAt())
                    .build())
        .collect(Collectors.toList());
  }

  public String renderTemplate(String template, Notification notification) {
    log.info("TEMPLATE: " + template);
    log.info("notification.getSender().getNickname(): " + notification.getSender().getNickname());
    return template
        .replace("{creator}", notification.getSender().getNickname())
        .replace("{project}", notification.getProject().getTitle());
  }

  @Transactional
  public void markAsRead(Long notificationId, Long userId) {
    Notification notification =
        notificationRepository
            .findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("알림 없음"));

    if (!notification.getReceiver().getId().equals(userId)) {
      throw new AccessDeniedException("본인의 알림만 읽음 처리할 수 있습니다.");
    }

    notification =
        notification.toBuilder().isProcessed(true).updatedAt(LocalDateTime.now()).build();
    notificationRepository.save(notification);
  }
}
