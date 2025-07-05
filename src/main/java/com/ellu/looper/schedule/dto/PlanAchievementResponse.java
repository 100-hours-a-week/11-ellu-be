package com.ellu.looper.schedule.dto;

import lombok.Builder;

@Builder
public record PlanAchievementResponse(
    String title, Long totalSchedules, Long achievedSchedules, double achievementRate) {}
