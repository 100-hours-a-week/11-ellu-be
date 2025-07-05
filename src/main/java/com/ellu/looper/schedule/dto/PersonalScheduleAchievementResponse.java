package com.ellu.looper.schedule.dto;

public record PersonalScheduleAchievementResponse(
    Long totalSchedules, Long completedSchedules, double achievementRate) {}
