package com.ellu.looper.service;

import com.ellu.looper.dto.AuthResponse;
import com.ellu.looper.dto.oauth.KakaoUserInfo;
import com.ellu.looper.entity.RefreshToken;
import com.ellu.looper.entity.User;
import com.ellu.looper.jwt.JwtExpiration;
import com.ellu.looper.jwt.JwtProvider;
import com.ellu.looper.repository.RefreshTokenRepository;
import com.ellu.looper.repository.UserRepository;
import com.ellu.looper.service.oauth.KakaoOAuthService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                  User newUser =
                      new User(
                          null,
                          null,
                          kakaoUserInfo.getEmail(),
                          "kakao",
                          kakaoUserInfo.getId(),
                          "default_profile.jpg", // default image
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
        .ifPresent(refreshTokenRepository::delete); // 기존 토큰 삭제 (있으면)

    RefreshToken refreshTokenEntity =
        RefreshToken.builder()
            .createdAt(LocalDateTime.now())
            .user(user)
            .refreshToken(jwtRefreshToken)
            .tokenExpiresAt(
                LocalDateTime.now().plusSeconds(JwtExpiration.REFRESH_TOKEN_EXPIRATION / 1000))
            .build();

    refreshTokenRepository.save(refreshTokenEntity);

    return new AuthResponse(jwtAccessToken, jwtRefreshToken, isNewUser);
  }

  @Transactional
  public void registerNickname(Long userId, String nickname) {
    if (nickname.length() < 1 || nickname.length() > 10) {
      throw new IllegalArgumentException("Nickname should be 1-10 letters.");
    }

    if (userRepository.findByNickname(nickname).isPresent()) {
      throw new RuntimeException("nickname_already_exists");
    }

    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("user_not_found"));

    user.updateNickname(nickname);
  }

  @Transactional
  public void logout(String refreshToken) {
    RefreshToken token =
        refreshTokenRepository
            .findByRefreshToken(refreshToken)
            .orElseThrow(() -> new RuntimeException("unauthorized"));
    refreshTokenRepository.delete(token);
  }

  @Transactional
  public AuthResponse refreshAccessToken(String oldRefreshToken) {
    RefreshToken savedToken =
        refreshTokenRepository
            .findByRefreshToken(oldRefreshToken)
            .orElseThrow(() -> new RuntimeException("invalid_refresh_token"));

    Long userId = jwtProvider.extractUserId(oldRefreshToken);

    String newAccessToken =
        jwtProvider.generateToken(userId, JwtExpiration.ACCESS_TOKEN_EXPIRATION);
    String newRefreshToken =
        jwtProvider.generateToken(userId, JwtExpiration.REFRESH_TOKEN_EXPIRATION);

    savedToken.updateToken(newRefreshToken);

    return new AuthResponse(newAccessToken, newRefreshToken, false);
  }
}
