package com.ellu.looper.auth.service;

import com.ellu.looper.auth.dto.AuthResponse;
import com.ellu.looper.auth.dto.TokenRefreshResponse;
import com.ellu.looper.auth.dto.oauth.KakaoUserInfo;
import com.ellu.looper.auth.entity.RefreshToken;
import com.ellu.looper.auth.repository.RefreshTokenRepository;
import com.ellu.looper.auth.service.oauth.KakaoOAuthService;
import com.ellu.looper.exception.JwtException;
import com.ellu.looper.exception.NicknameAlreadyExistsException;
import com.ellu.looper.jwt.JwtExpiration;
import com.ellu.looper.jwt.JwtProvider;
import com.ellu.looper.user.entity.User;
import com.ellu.looper.user.repository.UserRepository;
import com.ellu.looper.user.service.ProfileImageService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

  private final KakaoOAuthService kakaoOAuthService;
  private final UserRepository userRepository;
  private final JwtProvider jwtProvider;
  private final RefreshTokenRepository refreshTokenRepository;
  private final ProfileImageService profileImageService;

  @Transactional
  public AuthResponse loginOrSignUp(String provider, String accessToken) {
    if (!"kakao".equals(provider)) {
      throw new IllegalArgumentException("Unsupported provider");
    }

    KakaoUserInfo kakaoUserInfo = kakaoOAuthService.getUserInfo(accessToken);

    User user =
        userRepository
            .findByEmail(kakaoUserInfo.getEmail())
            .orElseGet(
                () -> {
                  String randomProfileImage = profileImageService.getRandomProfileImage();
                  User newUser =
                      new User(
                          null,
                          null,
                          null,
                          kakaoUserInfo.getEmail(),
                          "kakao",
                          kakaoUserInfo.getId(),
                          randomProfileImage,
                          LocalDateTime.now(),
                          LocalDateTime.now(),
                          null);
                  return userRepository.save(newUser);
                });

    boolean isNewUser = (user.getNickname() == null);

    String jwtAccessToken = jwtProvider.createAccessToken(user.getId());
    String jwtRefreshToken = jwtProvider.createRefreshToken(user.getId());

    refreshTokenRepository
        .findByUserId(user.getId())
        .ifPresent(
            existingToken -> {
              log.info("Deleting existing token: {}", existingToken);
              refreshTokenRepository.delete(existingToken);
              refreshTokenRepository.flush();
            });

    RefreshToken refreshTokenEntity =
        RefreshToken.builder()
            .createdAt(LocalDateTime.now())
            .user(user)
            .refreshToken(jwtRefreshToken)
            .tokenExpiresAt(
                LocalDateTime.now().plusSeconds(JwtExpiration.REFRESH_TOKEN_EXPIRATION / 1000))
            .build();

    RefreshToken token = refreshTokenRepository.save(refreshTokenEntity);
    log.info("token" + token.getRefreshToken());
    Long userId = jwtProvider.extractUserId(token.getRefreshToken());
    log.info("UserId {} logged in. ", userId);

    return new AuthResponse(jwtAccessToken, jwtRefreshToken, isNewUser);
  }

  @Transactional
  public void registerNickname(Long userId, String nickname) {
    if (nickname.isEmpty() || nickname.length() > 10) {
      throw new IllegalArgumentException("Nickname should be 1-10 letters.");
    }

    if (userRepository.findByNickname(nickname).isPresent()) {
      throw new NicknameAlreadyExistsException("nickname_already_exists");
    }

    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("user_not_found"));

    user.updateNickname(nickname);

    log.info("UserId {} registered his/her nickname. ", userId);
    userRepository.save(user);
  }

  @Transactional
  public void logout(String refreshToken) {
    RefreshToken token =
        refreshTokenRepository
            .findByRefreshToken(refreshToken)
            .orElseThrow(() -> new RuntimeException("unauthorized"));
    Long userId = jwtProvider.extractUserId(token.getRefreshToken());
    refreshTokenRepository.delete(token);
    log.info("UserId {} logged out. ", userId);
  }

  @Transactional
  public TokenRefreshResponse refreshAccessToken(
      String oldRefreshToken, HttpServletResponse response) {
    RefreshToken savedToken =
        refreshTokenRepository
            .findByRefreshToken(oldRefreshToken)
            .orElseThrow(() -> new RuntimeException("invalid_refresh_token"));

    Long userId;
    boolean refreshTokenExpired = false;

    try {
      userId = jwtProvider.extractUserId(oldRefreshToken); // 내부적으로 parseClaims 호출됨
      jwtProvider.validateToken(oldRefreshToken); // 만료되었으면 예외 발생
    } catch (JwtException e) {
      refreshTokenExpired = true;
      userId = jwtProvider.extractUserId(oldRefreshToken); // 만료된 토큰이라도 userId는 뽑을 수 있음
    }

    String newAccessToken = jwtProvider.createAccessToken(userId);

    if (refreshTokenExpired) {
      String newRefreshToken = jwtProvider.createRefreshToken(userId);
      savedToken.updateToken(newRefreshToken);

      // HttpOnly 쿠키로 새 refresh token 전달
      setTokenCookies(response, newRefreshToken);
    }

    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    TokenRefreshResponse.UserInfo userInfo =
        new TokenRefreshResponse.UserInfo(
            user.getId(),
            user.getNickname(),
            profileImageService.getProfileImageUrl(user.getFileName()));

    return new TokenRefreshResponse(newAccessToken, userInfo);
  }

  public void setTokenCookies(HttpServletResponse response, String refreshToken) {
    ResponseCookie refreshCookie =
        ResponseCookie.from("refresh_token", refreshToken)
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .path("/")
            .maxAge(JwtExpiration.REFRESH_TOKEN_EXPIRATION)
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
  }

  public String extractRefreshTokenFromCookies(HttpServletRequest request) {
    if (request.getCookies() == null) {
      return null;
    }

    for (Cookie cookie : request.getCookies()) {
      if ("refresh_token".equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }
}
