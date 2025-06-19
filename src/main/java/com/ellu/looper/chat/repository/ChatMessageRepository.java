package com.ellu.looper.chat.repository;

import com.ellu.looper.chat.entity.ChatMessage;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  @Query(
      "SELECT m FROM ChatMessage m WHERE m.user.id=:userId"
          + " AND m.createdAt >= :cutoff ORDER BY m.createdAt ASC")
  List<ChatMessage> findRecentMessages(
      @Param("userId") Long userId, @Param("cutoff") LocalDateTime cutoff);

  @Query("SELECT m FROM ChatMessage m")
  List<ChatMessage> findRecentMessages1(
      @Param("userId") Long userId, @Param("cutoff") LocalDateTime cutoff);
}
