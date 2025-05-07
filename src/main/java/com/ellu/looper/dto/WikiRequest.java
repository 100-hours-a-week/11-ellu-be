package com.ellu.looper.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiRequest {
  @NotBlank(message = "Content must not be empty")
  private String content;

  private Long projectId;
}
