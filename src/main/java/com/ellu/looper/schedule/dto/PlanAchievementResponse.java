package com.ellu.looper.schedule.dto;

import lombok.Builder;

@Builder
public record PlanAchievementResponse(
    String title, Long total_schedules, Long achieved_schedules, double achievement_rate) {}
