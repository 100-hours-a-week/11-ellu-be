package com.ellu.looper.kafka.dto;

import com.ellu.looper.schedule.dto.AssigneeDto;
import com.ellu.looper.schedule.dto.ProjectScheduleCreateRequest;
import com.ellu.looper.schedule.dto.StompProjectScheduleUpdateRequest;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleEventMessage {
  private String projectId;
  private String type; // SCHEDULE_CREATED, SCHEDULE_UPDATED, SCHEDULE_DELETED
  private ScheduleDto schedule;
  private Long userId;
  private Long scheduleId;
  private StompProjectScheduleUpdateRequest updateRequest;
  private ProjectScheduleCreateRequest createRequest;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder(toBuilder = true)
  public static class ScheduleDto {
    private Long id;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String position;
    private String description;
    private List<AssigneeDto> assignees;
  }
}
