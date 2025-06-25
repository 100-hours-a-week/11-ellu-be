package com.ellu.looper.chat.entity;

import com.ellu.looper.commons.enums.ChatRole;
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

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder(toBuilder = true)
@Table(
    name = "chat_messages",
    indexes = {
      @Index(
          name = "idx_message_conversation_id_createdat",
          columnList = "conversation_id, created_at")
    })
public class ChatMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "conversation_id", nullable = false)
  private ChatConversation conversation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "message_type")
  @Enumerated(EnumType.STRING)
  private ChatRole messageType;

  @Column(columnDefinition = "text", name = "content")
  private String content;

  @Column(columnDefinition = "jsonb", name = "metadata")
  private String metadata;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
