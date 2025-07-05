package com.ellu.looper.schedule.dto;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder(toBuilder = true)
public record DailyAchievementResponse(LocalDateTime date, Long created_schedules) {}
