package com.ellu.looper.chat.service;

import com.ellu.looper.chat.dto.MessageRequest;
import com.ellu.looper.kafka.ChatProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {
  private final ChatProducer chatProducer;

  public void sendChatMessage(MessageRequest request, Long userId) {
    chatProducer.sendUserMessage(userId, request);
  }
}
