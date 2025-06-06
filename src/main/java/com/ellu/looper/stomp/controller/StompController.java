package com.ellu.looper.stomp.controller;

import com.ellu.looper.commons.enums.NotificationType;
import com.ellu.looper.kafka.ScheduleEventProducer;
import com.ellu.looper.kafka.dto.ScheduleEventMessage;
import com.ellu.looper.schedule.dto.ProjectScheduleCreateRequest;
import com.ellu.looper.schedule.dto.ProjectScheduleResponse;
import com.ellu.looper.schedule.dto.ProjectScheduleTakeRequest;
import com.ellu.looper.schedule.dto.ProjectScheduleUpdateRequest;
import com.ellu.looper.schedule.entity.ProjectSchedule;
import com.ellu.looper.schedule.repository.ProjectScheduleRepository;
import com.ellu.looper.schedule.service.ProjectScheduleService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
public class StompController {

  private final SimpMessageSendingOperations messageTemplate;
  private final ProjectScheduleService projectScheduleService;
  private final ScheduleEventProducer scheduleEventProducer;
  private final ProjectScheduleRepository projectScheduleRepository;

  public StompController(
      SimpMessageSendingOperations messageTemplate,
      ProjectScheduleService projectScheduleService,
      ScheduleEventProducer scheduleEventProducer,
      ProjectScheduleRepository projectScheduleRepository) {
    this.messageTemplate = messageTemplate;
    this.projectScheduleService = projectScheduleService;
    this.scheduleEventProducer = scheduleEventProducer;
    this.projectScheduleRepository = projectScheduleRepository;
  }

  @MessageMapping("/{projectId}/update")
  // client에서 특정 /app/{projectId} 형태로 메시지를 publish 시 MessageMapping 수신
  public void handleScheduleUpdate(
      @DestinationVariable Long projectId,
      ProjectScheduleUpdateRequest scheduleUpdateRequest,
      Message<?> headers) {

    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(headers);

    Long userId = (Long) accessor.getSessionAttributes().get("userId");

    // 일정 수정 처리, 내부적으로 Notification 메시지 발행
    ProjectScheduleResponse projectScheduleResponse =
        projectScheduleService.updateSchedule(projectId, userId, scheduleUpdateRequest);

    // Kafka schedule 토픽에 일정 변경 이벤트 발행 (다중 인스턴스 WebSocket 브로드캐스트용)
    ScheduleEventMessage updateEvent =
        ScheduleEventMessage.builder()
            .projectId(projectId.toString())
            .type(NotificationType.SCHEDULE_UPDATED.name())
            .schedule(toDto(projectScheduleResponse))
            .build();
    scheduleEventProducer.sendScheduleEvent(updateEvent);

    // WebSocket 전파는 Kafka Consumer가 담당하므로 이곳에서는 수행하지 않음
  }

  @MessageMapping("/{projectId}/delete")
  public void handleScheduleDelete(
      @DestinationVariable Long projectId, Long scheduleId, Message<?> headers) {

    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(headers);

    Long userId = (Long) accessor.getSessionAttributes().get("userId");

    projectScheduleService.deleteSchedule(scheduleId, userId);

    ScheduleEventMessage deleteEvent =
        ScheduleEventMessage.builder()
            .projectId(projectId.toString())
            .type(NotificationType.SCHEDULE_DELETED.name())
            .schedule(ScheduleEventMessage.ScheduleDto.builder().id(scheduleId).build())
            .build();

    scheduleEventProducer.sendScheduleEvent(deleteEvent);
  }

  @MessageMapping("/{projectId}/create")
  public void createSchedule(
      @DestinationVariable Long projectId,
      ProjectScheduleCreateRequest createRequest,
      Message<?> headers) {

    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(headers);

    Long userId = (Long) accessor.getSessionAttributes().get("userId");
    List<ProjectScheduleResponse> createdList =
        projectScheduleService.createSchedules(projectId, userId, createRequest);

    for (ProjectScheduleResponse response : createdList) {
      ScheduleEventMessage event =
          ScheduleEventMessage.builder()
              .projectId(projectId.toString())
              .type(NotificationType.SCHEDULE_CREATED.name())
              .schedule(toDto(response))
              .build();

      scheduleEventProducer.sendScheduleEvent(event);
    }
  }

  // assignee 등록 + 개인 스케줄 생성
  @MessageMapping("/{projectId}/take")
  public void takeSchedule(
      @DestinationVariable Long projectId,
      ProjectScheduleTakeRequest takeRequest,
      Message<?> headers) {

    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(headers);

    Long userId = (Long) accessor.getSessionAttributes().get("userId");

    projectScheduleService.takeSchedule(projectId, takeRequest.projectScheduleId(), userId);

    // 반영된 스케줄 정보 조회
    ProjectSchedule updatedSchedule =
        projectScheduleRepository
            .findByIdAndDeletedAtIsNull(takeRequest.projectScheduleId())
            .orElseThrow(() -> new EntityNotFoundException("Project schedule not found"));

    // Kafka로 스케줄 업데이트 이벤트 발행
    ScheduleEventMessage event =
        ScheduleEventMessage.builder()
            .projectId(projectId.toString())
            .type("SCHEDULE_UPDATED")
            .schedule(toDto(projectScheduleService.toResponse(updatedSchedule)))
            .build();

    scheduleEventProducer.sendScheduleEvent(event);
  }

  private ScheduleEventMessage.ScheduleDto toDto(ProjectScheduleResponse response) {
    return ScheduleEventMessage.ScheduleDto.builder()
        .id(response.id())
        .title(response.title())
        .startTime(response.start_time())
        .endTime(response.end_time())
        .position(response.position())
        .description(response.description())
        .assignees(response.assignees())
        .build();
  }
}
