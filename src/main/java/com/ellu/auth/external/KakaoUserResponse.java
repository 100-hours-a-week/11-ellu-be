package com.ellu.auth.external;

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
