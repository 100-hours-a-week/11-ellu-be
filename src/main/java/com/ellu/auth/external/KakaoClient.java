package com.ellu.auth.external;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class KakaoClient {

  private final WebClient webClient = WebClient.create();

  public KakaoUserResponse getUserInfo(String authorizationHeader) {
    return webClient
        .get()
        .uri("https://kapi.kakao.com/v2/user/me")
        .header("Authorization", authorizationHeader)
        .retrieve()
        .bodyToMono(KakaoUserResponse.class)
        .block();
  }
}
