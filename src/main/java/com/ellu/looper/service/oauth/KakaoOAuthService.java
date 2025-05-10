package com.ellu.looper.service.oauth;

import com.ellu.looper.dto.oauth.KakaoUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j // 로깅 추가
public class KakaoOAuthService {

  private final WebClient webClient = WebClient.builder().baseUrl("https://kapi.kakao.com").build();

  public KakaoUserInfo getUserInfo(String accessToken) {
    try {
      return webClient
          .get()
          .uri("/v2/user/me")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .retrieve()
          .onStatus(
              HttpStatusCode::is4xxClientError,
              clientResponse -> {
                log.error("Kakao User Info API 4xx error: {}", clientResponse); // 에러 응답 로깅
                return clientResponse
                    .bodyToMono(String.class) // 에러 응답 본문도 로깅
                    .flatMap(
                        errorBody -> {
                          log.error("Kakao User Info API error body: {}", errorBody);
                          return Mono.error(
                              new RuntimeException(
                                  "Kakao User Info API error: " + clientResponse.statusCode()));
                        });
              })
          .bodyToMono(KakaoUserInfo.class)
          .block();
    } catch (WebClientResponseException e) {
      log.error("WebClient error during Kakao User Info API call", e); // WebClient 예외 로깅
      throw new RuntimeException("Error during Kakao User Info API call", e);
    }
  }
}
