package com.ellu.looper.fastapi.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder(toBuilder = true)
public class WikiEmbeddingResponse {
  private Long project_id;
  private String status;
  private LocalDateTime embedded_at;
}
