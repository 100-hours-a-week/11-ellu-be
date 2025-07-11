package com.ellu.looper.schedule.entity;

import com.ellu.looper.commons.enums.Color;
import com.ellu.looper.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    indexes = {
      @Index(name = "IDX_SCHEDULE_MEMBER_ID_DELETED_AT", columnList = "member_id, deleted_at"),
      @Index(name = "IDX_SCHEDULE_USER_COMPLETED", columnList = "member_id, is_completed"),
      @Index(name = "IDX_SCHEDULE_USER_CREATED_AT", columnList = "member_id, created_at")
      // CREATE INDEX idx_schedule_plan_stats ON schedule(user_id, plan_id, is_completed);
    })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Schedule {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_id")
  private Plan plan;

  @Column(nullable = false, length = 50)
  private String title;

  @Column private String description;

  @Enumerated(EnumType.STRING)
  private Color color;

  @Column(name = "is_completed", nullable = false)
  @Builder.Default
  private boolean isCompleted = false;

  @Column(name = "is_ai_recommended", nullable = false)
  @Builder.Default
  private boolean isAiRecommended = false;

  @Column(name = "start_time", nullable = false)
  private LocalDateTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalDateTime endTime;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public boolean isCompleted() {
    return this.isCompleted;
  }
}
