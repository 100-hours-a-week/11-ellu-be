package com.ellu.looper.dto.schedule;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ScheduleResponse(

    String title,

    String description,

    @JsonProperty("isCompleted")
    boolean completed,

    @JsonProperty("isAiRecommended")
    boolean aiRecommended,

    @JsonProperty("isProjectSchedule")
    boolean projectSchedule,

    LocalDateTime startTime,

    LocalDateTime endTime

) {

}

