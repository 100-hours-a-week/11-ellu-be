package com.ellu.looper.fastapi.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// AI 서버 응답
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder(toBuilder = true)
public class MeetingNoteResponse {

  @JsonProperty("message")
  private String message;

  @JsonProperty("detail")
  private List<SchedulePreview> detail;
}
