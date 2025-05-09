package com.ellu.looper.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// AI 서버 응답
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class MeetingNoteResponse {

  private String message;

  private List<SchedulePreview> data;

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  public static class SchedulePreview {
    private String keyword;
    private List<String> subtasks;
  }
}
