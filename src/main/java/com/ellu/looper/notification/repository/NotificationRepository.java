package com.ellu.looper.notification.repository;

import com.ellu.looper.notification.entity.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findByReceiverIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long receiverId);

  Optional<Notification> findByIdAndDeletedAtIsNull(Long notificationId);
}



