package com.ellu.looper.auth.repository;

import com.ellu.looper.auth.entity.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByUserId(Long userId);

  Optional<RefreshToken> findByRefreshToken(String refreshToken);
}
