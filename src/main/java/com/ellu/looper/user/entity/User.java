package com.ellu.looper.user.entity;

import com.ellu.looper.commons.util.HangulUtil;
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
@Table(name = "MEMBER",
indexes = {
    @Index(name = "idx_member_nickname", columnList = "nickname")
})
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false, updatable = false)
  private Long id;

  @Column(unique = true, length = 15)
  private String nickname;

  @Column(name = "nickname_choseong", length = 15)
  private String nicknameChoseong;

  @Column(unique = true, length = 255)
  private String email;

  @Column(length = 50)
  private String provider;

  @Column(name = "provider_id", length = 50)
  private String providerId;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public void updateNickname(String nickname) {
    this.nickname = nickname;
    this.nicknameChoseong = HangulUtil.extractChoseong(nickname);
    this.updatedAt = LocalDateTime.now();
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
    this.nicknameChoseong = HangulUtil.extractChoseong(nickname);
    this.createdAt = LocalDateTime.now();
  }

  public void setProfileImage(String fileName) {
    this.fileName = fileName;
    this.updatedAt = LocalDateTime.now();
  }
}
