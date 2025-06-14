package com.ellu.looper.chat.controller;

import com.ellu.looper.chat.dto.MessageRequest;
import com.ellu.looper.chat.service.ChatService;
import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.CurrentUser;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

  private final ChatService chatService;

  @PostMapping("/messages")
  public ResponseEntity<ApiResponse<?>> sendMessage(
      @CurrentUser Long userId, @RequestBody MessageRequest request) {
    request = request.toBuilder().user_id(String.valueOf(userId)).date(LocalDateTime.now()).build();
    chatService.sendChatMessage(request, userId);
    return ResponseEntity.accepted().body(ApiResponse.success("message_sent_to_chatbot", null));
  }
}
