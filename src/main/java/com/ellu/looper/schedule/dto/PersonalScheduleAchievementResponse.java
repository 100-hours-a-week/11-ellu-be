package com.ellu.looper.schedule.dto;

public record PersonalScheduleAchievementResponse(
    Long total_schedules, Long completed_schedules, double achievement_rate) {}
