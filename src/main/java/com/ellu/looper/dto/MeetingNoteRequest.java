package com.ellu.looper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingNoteRequest {
    private String meetingNote;
    private LocalDateTime createdAt;
    private Long authorId;
    private String nickname;
    private String position;
} 