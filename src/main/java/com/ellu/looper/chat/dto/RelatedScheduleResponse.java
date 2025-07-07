package com.ellu.looper.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record RelatedScheduleResponse(
    @NotBlank String task_title,
    @NotBlank String sub_title,
    @NotNull LocalDateTime start_at,
    @NotNull LocalDateTime end_at) {}
