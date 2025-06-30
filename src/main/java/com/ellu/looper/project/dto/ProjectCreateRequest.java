package com.ellu.looper.project.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class ProjectCreateRequest {

  @NotBlank(message = "Title must not be empty")
  private String title;

  private String color; // Hex Color String

  private String position; // 생성자 본인의 position

  private List<AddedMember> added_members;

  private String wiki;
}
