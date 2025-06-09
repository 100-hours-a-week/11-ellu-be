package com.ellu.looper.schedule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record StompProjectScheduleUpdateRequest(
    Long schedule_id,
    @NotBlank String title,
    String description,
    @NotNull LocalDateTime start_time,
    @NotNull LocalDateTime end_time,
    @JsonProperty("is_completed") Boolean completed,
    String position) {

}