package com.ellu.looper.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class AddedMember {
  @NotBlank(message = "Nickname must not be empty")
  private String nickname;

  @NotBlank(message = "Position must not be empty")
  private String position;
}