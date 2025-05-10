package com.ellu.looper.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenRefreshResponse {
  private String accessToken;
  private UserInfo user;

  @Getter
  @AllArgsConstructor
  public static class UserInfo {
    private Long id;
    private String nickname;
    private String imageUrl;
  }
}
