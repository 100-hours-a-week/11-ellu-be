package com.ellu.looper.dto.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoUserInfo {

  @JsonProperty("id")
  private String id;

  @JsonProperty("kakao_account")
  private KakaoAccount kakaoAccount;

  @Getter
  @NoArgsConstructor
  public static class KakaoAccount {
    private String email;
  }

  public String getEmail() {
    return kakaoAccount.getEmail();
  }
}
