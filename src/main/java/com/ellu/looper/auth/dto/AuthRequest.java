package com.ellu.looper.auth.dto;

import lombok.Getter;

@Getter
public class AuthRequest {
  private String provider;
  private String accessToken;
}
