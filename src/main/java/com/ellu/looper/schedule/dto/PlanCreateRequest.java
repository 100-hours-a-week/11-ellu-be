package com.ellu.looper.schedule.dto;

import com.ellu.looper.schedule.dto.ProjectScheduleCreateRequest.ProjectScheduleDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanCreateRequest {
  @NotEmpty(message = "chatbot_schedules must not be empty")
  @Valid
  private List<ProjectScheduleDto> chatbot_schedules;

  @NotNull private String plan_title;

  private String color;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProjectScheduleDto {

    @NotBlank(message = "title is required")
    private String title;

    private String description;

    @NotNull(message = "start_time is required")
    @JsonProperty("start_time")
    private LocalDateTime startTime;

    @NotNull(message = "end_time is required")
    @JsonProperty("end_time")
    private LocalDateTime endTime;
  }
}
