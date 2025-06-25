package com.ellu.looper.auth.entity;

import com.ellu.looper.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Table(
    name = "refresh_token",
    indexes = {
      @Index(name = "idx_refresh_token_member_id", columnList = "member_id"),
      @Index(name = "idx_refresh_token_refresh_token", columnList = "refresh_token")
    })
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private User user;

  @Column(name = "refresh_token", nullable = false, length = 255)
  private String refreshToken;

  @Column(name = "token_expires_at", nullable = false)
  private LocalDateTime tokenExpiresAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreatedDate
  private LocalDateTime createdAt;

  public void updateToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }
}
