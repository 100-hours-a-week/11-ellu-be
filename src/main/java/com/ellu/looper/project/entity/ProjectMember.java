package com.ellu.looper.project.entity;

import com.ellu.looper.commons.enums.Role;
import com.ellu.looper.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "project_member",
    indexes = {
        @Index(name = "idx_projectmember_project_id_deleted_at", columnList = "project_id, deleted_at"),
        @Index(name = "idx_projectmember_user_id_deleted_at", columnList = "member_id, deleted_at"),
        @Index(name = "idx_projectmember_project_position_deleted_at", columnList = "project_id, position, deleted_at")
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectMember {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private User user;

  @Setter
  @Column(name="position", length = 15)
  private String position; // FE, BE, CLOUD, etc

  @Column(nullable = false, length = 15)
  @Enumerated(EnumType.STRING)
  private Role role;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Setter
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
  }
}
