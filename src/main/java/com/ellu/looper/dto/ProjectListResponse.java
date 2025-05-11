package com.ellu.looper.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ProjectListResponse {
  private String title;
  private String color;
  private List<ProjectMemberResponse> members;
}
