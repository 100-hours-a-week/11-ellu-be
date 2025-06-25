package com.ellu.looper.auth.repository;

import com.ellu.looper.auth.entity.RefreshToken;
import io.lettuce.core.dynamic.annotation.Param;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {


  @Query("SELECT rt FROM RefreshToken rt JOIN FETCH rt.user WHERE rt.user.id = :userId")
  Optional<RefreshToken> findByUserId(@Param("userId") Long userId);

  Optional<RefreshToken> findByRefreshToken(String refreshToken);
}
