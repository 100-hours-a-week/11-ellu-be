package com.ellu.looper.dto.schedule;

import com.ellu.looper.commons.enums.Color;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder(toBuilder = true)
public record ProjectScheduleResponse(
    Long id,
    String title,
    String description,
    LocalDateTime start_time,
    LocalDateTime end_time,
    boolean is_completed,
    boolean is_project_schedule,
    Color color,
    List<AssigneeDto> assignees) {}
