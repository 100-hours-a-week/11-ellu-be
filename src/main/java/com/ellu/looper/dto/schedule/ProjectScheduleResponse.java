package com.ellu.looper.dto.schedule;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder(toBuilder = true)
public record ProjectScheduleResponse(
    Long id,
    String title,
    String description,
    LocalDateTime start_time,
    LocalDateTime end_time,
    boolean is_completed,
    boolean is_project_schedule
) {}
