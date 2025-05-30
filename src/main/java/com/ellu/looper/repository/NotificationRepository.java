package com.ellu.looper.repository;

import com.ellu.looper.entity.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findByReceiverIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long receiverId);
}
