<<<<<<<< HEAD:src/main/java/com/ellu/looper/fastapi/dto/MeetingNoteResponse.java
package com.ellu.looper.fastapi.dto;
========
package com.ellu.looper.project.dto;
>>>>>>>> main:src/main/java/com/ellu/looper/project/dto/MeetingNoteResponse.java

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

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  public static class SchedulePreview {
    @JsonProperty("position")
    private String position;

    @JsonProperty("task")
    private String task;

    @JsonProperty("subtasks")
    private List<String> subtasks;
  }
}
