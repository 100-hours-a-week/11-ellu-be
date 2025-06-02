package com.ellu.looper.schedule.dto;

import java.util.List;

public record ProjectScheduleTakeRequest(
    List<Long> projectScheduleIds
) {}
