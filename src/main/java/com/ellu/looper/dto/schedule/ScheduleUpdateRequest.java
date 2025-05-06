package com.ellu.looper.dto.schedule;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record ScheduleUpdateRequest(

    String title,

    String description,

    @JsonProperty("is_completed")
    Boolean completed,

    @JsonProperty("start_time")
    LocalDateTime startTime,

    @JsonProperty("end_time")
    LocalDateTime endTime
) {

}

