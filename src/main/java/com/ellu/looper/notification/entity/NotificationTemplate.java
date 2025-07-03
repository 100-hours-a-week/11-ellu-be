package com.ellu.looper.notification.entity;

import com.ellu.looper.commons.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "notification_template",
    indexes = {@Index(name = "IDX_NOTIFICATION_TEMPLATE_TYPE", columnList = "type")})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class NotificationTemplate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String template;

  @Column(length = 20, nullable = false)
  @Enumerated(EnumType.STRING)
  private NotificationType type;
}
