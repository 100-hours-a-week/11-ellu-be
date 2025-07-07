package com.ellu.looper.chat.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record RelatedScheduleRequest(
    @NotNull Long user_id,
    @NotNull LocalDateTime start,
    @NotNull LocalDateTime end,
    String task_title_keyword,
    String category) {}
