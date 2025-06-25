package com.ellu.looper.notification.entity;

import com.ellu.looper.commons.JsonConverter;
import com.ellu.looper.project.entity.Project;
import com.ellu.looper.user.entity.User;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.*;
import org.hibernate.annotations.Type;

@Entity
@Table(
    name = "notification",
    indexes = {
      @Index(
          name = "idx_notification_receiver_id_deleted_at_created_at",
          columnList = "receiver_id, deleted_at, created_at DESC"),
      @Index(name = "idx_notification_deleted_at_created_at", columnList = "deleted_at, created_at")
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

  public void softDelete(LocalDateTime now) {
    this.deletedAt = now;
  }
}
