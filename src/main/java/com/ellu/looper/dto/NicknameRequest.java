package com.ellu.looper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class NicknameRequest {

  @NotBlank
  @Size(min = 1, max = 10)
  private String nickname;
}
