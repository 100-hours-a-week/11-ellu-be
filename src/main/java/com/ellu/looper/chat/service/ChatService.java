package com.ellu.looper.chat.service;

import com.ellu.looper.chat.dto.ChatMessageResponse;
import com.ellu.looper.chat.dto.MessageRequest;
import com.ellu.looper.chat.entity.ChatConversation;
import com.ellu.looper.chat.entity.ChatMessage;
import com.ellu.looper.chat.repository.ChatConversationRepository;
import com.ellu.looper.chat.repository.ChatMessageRepository;
import com.ellu.looper.kafka.ChatProducer;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

  private final ChatProducer chatProducer;
  private final ChatMessageRepository chatMessageRepository;
  private final ChatConversationRepository conversationRepository;

  public void sendChatMessage(MessageRequest request, Long userId) {
    chatProducer.sendUserMessage(userId, request);
  }

  // 지난 24시간의 대화 기록 조회
  public List<ChatMessageResponse> getRecentHistory(Long userId) {
    log.info("UserId {} fetched chat history. ", userId);
    LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
    ChatConversation recentConversation =
        conversationRepository.findTop1ByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            userId, cutoff);

    if (recentConversation == null) {
      return Collections.emptyList();
    }

    List<ChatMessage> messages =
        chatMessageRepository.findConversationMessages(recentConversation.getId());
    return messages.stream()
        .map(msg -> new ChatMessageResponse(msg.getMessageType().name(), msg.getContent()))
        .collect(Collectors.toList());
  }
}
