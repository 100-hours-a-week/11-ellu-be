package com.ellu.looper.notification.repository;

import com.ellu.looper.notification.entity.Notification;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  @EntityGraph(attributePaths = {"sender", "template"})
  List<Notification> findByReceiverIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long receiverId);

  @EntityGraph(attributePaths = {"sender", "template"})
  Optional<Notification> findByIdAndDeletedAtIsNull(Long notificationId);

  List<Notification> findByCreatedAtBeforeAndDeletedAtIsNull(LocalDateTime oneWeekAgo);
}
