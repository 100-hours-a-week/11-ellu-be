package com.ellu.looper.dto.schedule;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ScheduleResponse(
    Long id,
    String title,
    String description,
    @JsonProperty("is_completed") boolean completed,
    @JsonProperty("is_ai_recommended") boolean aiRecommended,
    @JsonProperty("is_project_schedule") boolean projectSchedule,
    @JsonProperty("start_time") LocalDateTime startTime,
    @JsonProperty("end_time") LocalDateTime endTime) {}
