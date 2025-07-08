package com.ellu.looper.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class SchedulePreview {
  @JsonProperty("position")
  private String position;

  @JsonProperty("task")
  private String task;

  @JsonProperty("subtasks")
  private List<String> subtasks;
}
