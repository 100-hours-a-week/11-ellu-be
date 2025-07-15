package com.ellu.looper.stomp.service;

import com.ellu.looper.commons.enums.NotificationType;
import com.ellu.looper.kafka.ScheduleEventProducer;
import com.ellu.looper.kafka.dto.ScheduleEventMessage;
import com.ellu.looper.schedule.dto.ProjectScheduleCreateRequest;
import com.ellu.looper.schedule.dto.ProjectScheduleTakeRequest;
import com.ellu.looper.schedule.dto.StompProjectScheduleUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class StompService {

  private final ScheduleEventProducer scheduleEventProducer;
  @Transactional
  public void updateSchedule(
      Long projectId, StompProjectScheduleUpdateRequest scheduleUpdateRequest, Long userId) {
    // Kafka schedule 토픽에 일정 변경 이벤트 발행 (다중 인스턴스 WebSocket 브로드캐스트용)
    ScheduleEventMessage updateEvent =
        ScheduleEventMessage.builder()
            .scheduleId(scheduleUpdateRequest.schedule_id())
            .userId(userId)
            .updateRequest(scheduleUpdateRequest)
            .projectId(String.valueOf(projectId))
            .type(NotificationType.SCHEDULE_UPDATED.name())
            .build();
    scheduleEventProducer.sendScheduleEvent(updateEvent);

    // WebSocket 전파는 Kafka Consumer가 담당하므로 이곳에서는 수행하지 않음

  }

  @Transactional
  public void deleteSchedule(Long projectId, Long scheduleId, Long userId) {
    ScheduleEventMessage deleteEvent =
        ScheduleEventMessage.builder()
            .scheduleId(scheduleId)
            .userId(userId)
            .projectId(projectId.toString())
            .type(NotificationType.SCHEDULE_DELETED.name())
            .schedule(ScheduleEventMessage.ScheduleDto.builder().id(scheduleId).build())
            .build();

    scheduleEventProducer.sendScheduleEvent(deleteEvent);
  }

  @Transactional
  public void createSchedule(
      Long projectId, ProjectScheduleCreateRequest createRequest, Long userId) {
    {
      ScheduleEventMessage event =
          ScheduleEventMessage.builder()
              .projectId(projectId.toString())
              .userId(userId)
              .createRequest(createRequest)
              .type(NotificationType.SCHEDULE_CREATED.name())
              .build();

      scheduleEventProducer.sendScheduleEvent(event);
    }
  }

  @Transactional
  public void takeSchedule(Long projectId, ProjectScheduleTakeRequest takeRequest, Long userId) {
    // Kafka로 스케줄 업데이트 이벤트 발행
    ScheduleEventMessage event =
        ScheduleEventMessage.builder()
            .projectId(projectId.toString())
            .scheduleId(takeRequest.schedule_id())
            .userId(userId)
            .type(NotificationType.SCHEDULE_TAKEN.name())
            .build();

    scheduleEventProducer.sendScheduleEvent(event);
  }
}
