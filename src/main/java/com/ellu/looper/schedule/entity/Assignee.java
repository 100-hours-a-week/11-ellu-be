package com.ellu.looper.schedule.entity;

import com.ellu.looper.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "assignee",
    indexes = {
      @Index(name = "IDX_ASSIGNEE_MEMBER_ID", columnList = "member_id"),
      @Index(name = "IDX_ASSIGNEE_PROJECT_SCHEDULE_ID", columnList = "project_schedule_id")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignee {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_schedule_id", nullable = false)
  private ProjectSchedule projectSchedule;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_id", nullable = false)
  private User user;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "has_taken_schedule")
  private Boolean hasTakenSchedule;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
  }

  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
  }

  @Builder(toBuilder = true)
  public Assignee(ProjectSchedule projectSchedule, User user) {
    this.projectSchedule = projectSchedule;
    this.user = user;
  }
}
