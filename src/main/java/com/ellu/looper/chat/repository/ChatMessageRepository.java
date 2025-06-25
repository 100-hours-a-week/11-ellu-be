package com.ellu.looper.chat.repository;

import com.ellu.looper.chat.entity.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
  @Query(
      "SELECT m FROM ChatMessage m WHERE m.conversation.id=:conversationId"
          + " AND m.content NOT LIKE '{\"task_title%'"
          + "ORDER BY m.createdAt ASC")
  List<ChatMessage> findConversationMessages(@Param("conversationId") Long conversationId);
}
