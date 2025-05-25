package com.ellu.looper.repository;

import com.ellu.looper.commons.enums.NotificationType;
import com.ellu.looper.entity.NotificationTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

  Optional<NotificationTemplate> findByType(NotificationType notificationType);
}
