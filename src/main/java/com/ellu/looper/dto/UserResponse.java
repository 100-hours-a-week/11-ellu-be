package com.ellu.looper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {
  private Long id;
  private String nickname;
  private String createdAt;
  private String imageUrl;
}
