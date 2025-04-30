package com.ellu.looper.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
  private String accessToken;
  private String refreshToken;
  private boolean isNewUser;
}
