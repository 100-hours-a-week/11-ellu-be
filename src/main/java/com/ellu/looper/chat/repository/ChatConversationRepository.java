package com.ellu.looper.chat.repository;

import com.ellu.looper.chat.entity.ChatConversation;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
  ChatConversation findTop1ByUserIdOrderByCreatedAtDesc(Long userId);
}
