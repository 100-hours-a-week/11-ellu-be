package com.ellu.looper.stomp.service;

import com.ellu.looper.commons.enums.NotificationType;
import com.ellu.looper.kafka.ScheduleEventProducer;
import com.ellu.looper.kafka.dto.ScheduleEventMessage;
import com.ellu.looper.schedule.dto.ProjectScheduleCreateRequest;
import com.ellu.looper.schedule.dto.ProjectScheduleResponse;
import com.ellu.looper.schedule.dto.ProjectScheduleTakeRequest;
import com.ellu.looper.schedule.dto.StompProjectScheduleUpdateRequest;
import com.ellu.looper.schedule.entity.ProjectSchedule;
import com.ellu.looper.schedule.repository.ProjectScheduleRepository;
import com.ellu.looper.schedule.service.ProjectScheduleService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class StompService {

  private final ProjectScheduleService projectScheduleService;
  private final ScheduleEventProducer scheduleEventProducer;
  private final ProjectScheduleRepository projectScheduleRepository;

  public void updateSchedule(
      Long projectId, StompProjectScheduleUpdateRequest scheduleUpdateRequest, Long userId) {

    // 일정 수정 처리, 내부적으로 Notification 메시지 발행
    ProjectScheduleResponse projectScheduleResponse =
        projectScheduleService.updateSchedule(
            scheduleUpdateRequest.schedule_id(), userId, scheduleUpdateRequest);

    // Kafka schedule 토픽에 일정 변경 이벤트 발행 (다중 인스턴스 WebSocket 브로드캐스트용)
    ScheduleEventMessage updateEvent =
        ScheduleEventMessage.builder()
            .projectId(String.valueOf(projectId))
            .type(NotificationType.SCHEDULE_UPDATED.name())
            .schedule(toDto(projectScheduleResponse))
            .build();
    scheduleEventProducer.sendScheduleEvent(updateEvent);

    // WebSocket 전파는 Kafka Consumer가 담당하므로 이곳에서는 수행하지 않음

  }

  public void deleteSchedule(
      Long projectId, Long scheduleId, ProjectScheduleTakeRequest deleteRequest, Long userId) {
    projectScheduleService.deleteSchedule(scheduleId, userId);

    ScheduleEventMessage deleteEvent =
        ScheduleEventMessage.builder()
            .projectId(projectId.toString())
            .type(NotificationType.SCHEDULE_DELETED.name())
            .schedule(ScheduleEventMessage.ScheduleDto.builder().id(scheduleId).build())
            .build();

    scheduleEventProducer.sendScheduleEvent(deleteEvent);
  }

  public void createSchedule(
      Long projectId, ProjectScheduleCreateRequest createRequest, Long userId) {
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

  public void takeSchedule(Long projectId, ProjectScheduleTakeRequest takeRequest, Long userId) {
    projectScheduleService.takeSchedule(projectId, takeRequest.schedule_id(), userId);

    // 반영된 스케줄 정보 조회
    ProjectSchedule updatedSchedule =
        projectScheduleRepository
            .findWithDetailsById(takeRequest.schedule_id())
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
