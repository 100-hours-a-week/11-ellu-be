package com.ellu.looper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
  private String accessToken;
  private String refreshToken;

  @JsonProperty("new_user")
  private boolean newUser;
}
