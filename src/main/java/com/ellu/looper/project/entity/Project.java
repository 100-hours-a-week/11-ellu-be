package com.ellu.looper.project.entity;

import com.ellu.looper.commons.enums.Color;
import com.ellu.looper.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder(toBuilder = true)
public class Project {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private User member;

  @Column private String title;

  @Enumerated(EnumType.STRING)
  private Color color;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Basic(fetch = FetchType.LAZY)
  @Column(columnDefinition = "TEXT")
  private String wiki;

  public void setDeletedAt(LocalDateTime now) {
    this.deletedAt = now;
  }
}
