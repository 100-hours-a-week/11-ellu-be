package com.ellu.looper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class UserResponse {
  private Long id;
  private String nickname;
  private String imageUrl;
}
