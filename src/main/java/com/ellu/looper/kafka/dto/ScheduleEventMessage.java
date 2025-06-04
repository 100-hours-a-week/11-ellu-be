package com.ellu.looper.kafka.dto;

import com.ellu.looper.schedule.dto.AssigneeDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleEventMessage {
    private String projectId;
    private String type; // SCHEDULE_CREATED, SCHEDULE_UPDATED, SCHEDULE_DELETED
    private ScheduleDto schedule;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleDto {
        private Long id;
        private String title;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String position;
        private String description;
        private List<AssigneeDto> assignees;
    }
} 