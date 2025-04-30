package com.ellu.looper.jwt;

import com.ellu.looper.entity.RefreshToken;
import com.ellu.looper.repository.RefreshTokenRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JwtService {
  private final RefreshTokenRepository refreshTokenRepository;

  public JwtService(RefreshTokenRepository refreshTokenRepository) {
    this.refreshTokenRepository = refreshTokenRepository;
  }

  @Transactional
  public void updateRefreshToken(Long memberId, String newRefreshToken) {
    // Find the refresh token by member ID
    Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByUserId(memberId);

    if (refreshTokenOpt.isPresent()) {
      // If the refresh token exists, update the refresh token value
      RefreshToken refreshToken = refreshTokenOpt.get();
      refreshToken.updateToken(newRefreshToken);
      refreshTokenRepository.save(refreshToken);
    } else {
      throw new IllegalArgumentException("No refresh token found for member with ID " + memberId);
    }
  }
}
