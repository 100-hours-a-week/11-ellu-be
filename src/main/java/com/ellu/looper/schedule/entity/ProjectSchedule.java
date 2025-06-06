package com.ellu.looper.schedule.entity;

import com.ellu.looper.project.entity.Project;
import com.ellu.looper.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "project_schedule",
    indexes = {
      @Index(name = "IDX_PROJECT_SCHEDULE_DELETED_AT", columnList = "deletedAt"),
      @Index(name = "IDX_PROJECT_SCHEDULE_MEMBER_ID", columnList = "member_id"),
      @Index(name = "IDX_PROJECT_SCHEDULE_IS_COMPLETED", columnList = "isCompleted")
    })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ProjectSchedule {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(length = 15)
  private String position;

  @Column(name = "start_time", nullable = false)
  private LocalDateTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalDateTime endTime;

  @Column(name = "is_completed", nullable = false)
  @Builder.Default
  private boolean isCompleted = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @OneToMany(mappedBy = "projectSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Assignee> assignees = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = this.createdAt;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
  }

  public void update(
      String title,
      String description,
      LocalDateTime startTime,
      LocalDateTime endTime,
      String position,
      Boolean isCompleted) {
    if (title != null) {
      this.title = title;
    }
    if (description != null) {
      this.description = description;
    }
    if (startTime != null) {
      this.startTime = startTime;
    }
    if (endTime != null) {
      this.endTime = endTime;
    }
    if (position != null) {
      this.position = position;
    }
    if (isCompleted != null) {
      this.isCompleted = isCompleted;
    }
  }

  public void addAssignee(Assignee assignee) {
    assignee.setProjectSchedule(this);
    this.assignees.add(assignee);
  }
}
