package com.ellu.looper.controller;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.CurrentUser;
import com.ellu.looper.dto.AuthRequest;
import com.ellu.looper.dto.AuthResponse;
import com.ellu.looper.dto.LogoutRequest;
import com.ellu.looper.dto.NicknameRequest;
import com.ellu.looper.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final AuthService authService;

  private final String clientId = "44153da72adabc7e47959244ebe53bac"; // 카카오 REST API 키
  private final String redirectUri = "http://localhost:8080/auth/kakao/callback";

  @GetMapping("/auth/kakao/callback")
  public ResponseEntity<AuthResponse> kakaoCallback(@RequestParam("code") String code) {
    log.info("Authorization Code: " + code);
    String accessToken = requestAccessToken(code);
    log.info("Access Token: " + accessToken);

    // 1. access token으로 로그인/회원가입 처리
    AuthResponse authResponse = authService.loginOrSignUp("kakao", accessToken);

    // 2. access token, refresh token, isNewUser 등을 JSON 형태로 응답
    return ResponseEntity.ok(authResponse);
  }

  private String requestAccessToken(String code) {
    RestTemplate restTemplate = new RestTemplate();
    String tokenUrl = "https://kauth.kakao.com/oauth/token";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON)); // 꼭 추가

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "authorization_code");
    params.add("client_id", clientId);
    params.add("redirect_uri", redirectUri);
    params.add("code", code);

    HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(tokenUrl, HttpMethod.POST, requestEntity, String.class);

      log.info("Response from Kakao token API: {}", response.getBody());

      if (response.getStatusCode() == HttpStatus.OK) {
        // JSON 파싱
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> resultMap = mapper.readValue(response.getBody(), Map.class);
        return (String) resultMap.get("access_token");
      } else {
        throw new RuntimeException(
            "Failed to get access token from Kakao: " + response.getStatusCode());
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Error during Kakao access token request", e);
    }
  }

  @PostMapping("/auth/token")
  public ResponseEntity<?> kakaoLogin(@RequestBody AuthRequest request) {
    return ResponseEntity.ok(
        authService.loginOrSignUp(request.getProvider(), request.getAccessToken()));
  }

  @DeleteMapping("/auth/token")
  public ResponseEntity<ApiResponse<Void>> logout(@RequestBody LogoutRequest request) {
    authService.logout(request.getRefreshToken());
    return ResponseEntity.ok(new ApiResponse("로그아웃 성공", null));
  }

  @PostMapping("/auth/users/nickname")
  public ResponseEntity<?> registerNickname(
      @CurrentUser Long userId, @RequestBody NicknameRequest request) {
    authService.registerNickname(userId, request.getNickname());
    return ResponseEntity.ok(new ApiResponse("닉네임 등록 완료", null));
  }

  @PostMapping("/auth/token/refresh")
  public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String refreshTokenHeader) {
    String refreshToken = refreshTokenHeader.replace("Bearer ", "");
    return ResponseEntity.ok(authService.refreshAccessToken(refreshToken));
  }
}
