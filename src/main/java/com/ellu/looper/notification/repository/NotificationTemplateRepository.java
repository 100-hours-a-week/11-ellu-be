package com.ellu.looper.notification.repository;

import com.ellu.looper.commons.enums.NotificationType;
import com.ellu.looper.notification.entity.NotificationTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

  Optional<NotificationTemplate> findByType(NotificationType notificationType);

  boolean existsByType(NotificationType key);
}
