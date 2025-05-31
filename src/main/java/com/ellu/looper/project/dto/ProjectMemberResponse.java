package com.ellu.looper.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ProjectMemberResponse {
  private String nickname;
  private String profileImageUrl;
}
