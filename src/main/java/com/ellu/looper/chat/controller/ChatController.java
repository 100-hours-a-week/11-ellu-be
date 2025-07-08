package com.ellu.looper.chat.controller;

import com.ellu.looper.chat.dto.ChatMessageResponse;
import com.ellu.looper.chat.dto.MessageRequest;
import com.ellu.looper.chat.dto.RelatedScheduleRequest;
import com.ellu.looper.chat.dto.RelatedScheduleResponse;
import com.ellu.looper.chat.service.ChatService;
import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

  @GetMapping("/history") // 해당 사용자의 지난 24시간동안의 채팅 기록 조회
  public ResponseEntity<ApiResponse<?>> getRecentHistory(@CurrentUser Long userId) {
    List<ChatMessageResponse> history = chatService.getRecentHistory(userId);
    return ResponseEntity.ok(ApiResponse.success("history_fetched", Map.of("history", history)));
  }

  @PostMapping("/query")
  public ResponseEntity<ApiResponse<?>> getRelatedSchedules(
      @RequestBody RelatedScheduleRequest request) {
    List<RelatedScheduleResponse> relatedSchedules = chatService.getRelatedSchedules(request);
    return ResponseEntity.ok(ApiResponse.success("calendar_query_result", relatedSchedules));
  }
}
