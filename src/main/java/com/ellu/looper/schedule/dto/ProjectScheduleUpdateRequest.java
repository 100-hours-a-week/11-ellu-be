package com.ellu.looper.schedule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectScheduleUpdateRequest(
    @NotBlank String title,
    String description,
    @NotNull LocalDateTime start_time,
    @NotNull LocalDateTime end_time,
    @JsonProperty("is_completed") Boolean completed,
    List<String> assignees) {}
