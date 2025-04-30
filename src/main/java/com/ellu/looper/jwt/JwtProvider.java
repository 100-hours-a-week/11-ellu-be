package com.ellu.looper.jwt;

import com.ellu.looper.exception.JwtException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

  @Value("${jwt.secret}")
  private String secretKeyPlainText;

  private Key secretKey;

  @PostConstruct
  protected void init() {
    this.secretKey = Keys.hmacShaKeyFor(secretKeyPlainText.getBytes(StandardCharsets.UTF_8));
  }

  public String generateToken(Long userId, long expirationMillis) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expirationMillis);

    return Jwts.builder()
        .subject(String.valueOf(userId))
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();
  }

  public String createAccessToken(Long id) {
    return generateToken(id, JwtExpiration.ACCESS_TOKEN_EXPIRATION);
  }

  public String createRefreshToken(Long id) {
    return generateToken(id, JwtExpiration.REFRESH_TOKEN_EXPIRATION);
  }

  public Long extractUserId(String token) {
    Claims claims = parseClaims(token);
    return Long.valueOf(claims.getSubject());
  }

  public boolean validateToken(String token) {
    parseClaims(token);
    return true;
  }

  private Claims parseClaims(String token) {
    try {
      return (Claims)
          Jwts.parser()
              .verifyWith((SecretKey) secretKey)
              .build()
              .parseSignedClaims(token)
              .getPayload();
    } catch (ExpiredJwtException e) {
      throw new JwtException("Token expired", HttpStatus.UNAUTHORIZED);
    } catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException e) {
      throw new JwtException("Invalid token", HttpStatus.UNAUTHORIZED);
    } catch (io.jsonwebtoken.security.SignatureException e) {
      throw new JwtException("Invalid signature", HttpStatus.UNAUTHORIZED);
    }
  }
}
