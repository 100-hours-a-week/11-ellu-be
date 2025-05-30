package com.ellu.looper.notification.entity;

import com.ellu.looper.commons.JsonConverter;
import com.ellu.looper.project.entity.Project;
import com.ellu.looper.user.entity.User;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import java.util.Map;
import lombok.*;

import java.time.LocalDateTime;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "notification",
    indexes = {
        @Index(name = "IDX_NOTIFICATION_DELETED_AT", columnList = "deleted_at"),
        @Index(name = "IDX_NOTIFICATION_IS_PROCESSED", columnList = "is_processed"),
        @Index(name = "IDX_NOTIFICATION_INVITE_STATUS", columnList = "invite_status")
    })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Sender
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id", nullable = false)
  private User sender;

  // Receiver
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "receiver_id", nullable = false)
  private User receiver;

  // Project
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  // Template
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id", nullable = false)
  private NotificationTemplate template;

  @Convert(converter = JsonConverter.class)
  @Column(columnDefinition = "JSON")
  @Type(JsonType.class)
  private Map<String, Object> payload;

  @Column(name = "invite_status", length = 20)
  private String inviteStatus;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
