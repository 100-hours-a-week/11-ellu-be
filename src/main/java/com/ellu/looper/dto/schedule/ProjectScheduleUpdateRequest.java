package com.ellu.looper.dto.schedule;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ProjectScheduleUpdateRequest(
    @NotBlank String title,
    @NotNull LocalDateTime start_time,
    @NotNull LocalDateTime end_time,
    @JsonProperty("is_completed")
    Boolean completed
) {}

