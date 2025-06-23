package com.ellu.looper.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KakaoUserResponse {
  private Long id;
  private KakaoAccount kakao_account;

  @Getter
  @Setter
  public static class KakaoAccount {
    private String email;
  }
}
