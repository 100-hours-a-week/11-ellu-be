package com.ellu.looper.dto.schedule;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ScheduleCreateRequest(

    @NotBlank String title,

    String description,

    @JsonProperty("is_ai_recommended")
    boolean aiRecommended,

    @JsonProperty("start_time")
    @NotNull LocalDateTime startTime,

    @JsonProperty("end_time")
    @NotNull LocalDateTime endTime

) {

}