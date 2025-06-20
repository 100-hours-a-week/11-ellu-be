package com.ellu.looper.stomp.controller;

import com.ellu.looper.schedule.dto.ProjectScheduleCreateRequest;
import com.ellu.looper.schedule.dto.ProjectScheduleTakeRequest;
import com.ellu.looper.schedule.dto.StompProjectScheduleUpdateRequest;
import com.ellu.looper.schedule.service.ProjectScheduleService;
import com.ellu.looper.stomp.service.StompService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class StompController {

  private final ProjectScheduleService projectScheduleService;
  private final StompService stompService;

  @MessageMapping("/{projectId}/update")
  // client에서 특정 /app/{projectId} 형태로 메시지를 publish 시 MessageMapping 수신
  public void handleScheduleUpdate(
      @DestinationVariable Long projectId,
      StompProjectScheduleUpdateRequest scheduleUpdateRequest,
      Message<?> headers) {
    Long userId = extractUserId(headers);
    stompService.updateSchedule(projectId, scheduleUpdateRequest, userId);
  }

  @MessageMapping("/{projectId}/delete")
  public void handleScheduleDelete(
      @DestinationVariable Long projectId,
      ProjectScheduleTakeRequest deleteRequest,
      Message<?> headers) {
    Long scheduleId = deleteRequest.schedule_id();
    Long userId = extractUserId(headers);
    stompService.deleteSchedule(projectId, scheduleId, deleteRequest, userId);
  }

  @MessageMapping("/{projectId}/create")
  public void createSchedule(
      @DestinationVariable Long projectId,
      ProjectScheduleCreateRequest createRequest,
      Message<?> headers) {
    Long userId = extractUserId(headers);
    stompService.createSchedule(projectId, createRequest, userId);
  }

  // assignee 등록 + 개인 스케줄 생성
  @MessageMapping("/{projectId}/take")
  public void takeSchedule(
      @DestinationVariable Long projectId,
      ProjectScheduleTakeRequest takeRequest,
      Message<?> headers) {
    Long userId = extractUserId(headers);
    stompService.takeSchedule(projectId, takeRequest, userId);
  }

  private Long extractUserId(Message<?> headers) {
    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(headers);
    Long userId = (Long) accessor.getSessionAttributes().get("userId");
    return userId;
  }
}
