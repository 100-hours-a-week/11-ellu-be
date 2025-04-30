package com.ellu.looper.jwt;

import com.ellu.looper.exception.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtProvider jwtProvider;
  private final JwtService jwtService; // 토큰 저장소 (DB or Memory 등)

  public JwtAuthenticationFilter(JwtProvider jwtProvider, JwtService jwtService) {
    this.jwtProvider = jwtProvider;
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    String token = null;

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      token = authHeader.substring(7);
    }

    if (token != null) {
      try {
        jwtProvider.validateToken(token);
        Long userId = jwtProvider.extractUserId(token);

        // 정상 인증
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(userId, null, null);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("JwtAuthenticationFilter 실행됨. 인증된 유저 ID: {}", userId);
        log.info("Authorization Header: {}", authHeader);
        log.info("토큰에서 추출된 사용자 ID: {}", userId);

      } catch (JwtException e) {
        if ("Token expired".equals(e.getMessage())) {
          // Access Token이 만료된 경우
          handleTokenExpired(request, response);
          return;
        } else {
          throw e; // 다른 에러는 그대로 던짐
        }
      }
    }

    filterChain.doFilter(request, response);
  }

  private void handleTokenExpired(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String refreshToken = request.getHeader("Refresh-Token");

    if (refreshToken != null && jwtProvider.validateToken(refreshToken)) {
      Long userId = jwtProvider.extractUserId(refreshToken);

      // Refresh Token도 유효한 경우: Access Token 재발급
      String newAccessToken =
          jwtProvider.generateToken(userId, JwtExpiration.ACCESS_TOKEN_EXPIRATION);
      String newRefreshToken =
          jwtProvider.generateToken(userId, JwtExpiration.REFRESH_TOKEN_EXPIRATION);

      // Refresh Token 갱신 저장
      jwtService.updateRefreshToken(userId, newRefreshToken);

      // 새 토큰을 응답 헤더에 담기
      response.setHeader("Authorization", "Bearer " + newAccessToken);
      response.setHeader("Refresh-Token", newRefreshToken);

      response.setStatus(HttpStatus.UNAUTHORIZED.value()); // 새 토큰을 받아 다시 요청해야 함
      response.getWriter().write("{ \"message\": \"Access token refreshed\" }");
      response.getWriter().flush();
    } else {
      // Refresh Token도 만료
      throw new JwtException("Refresh token expired", HttpStatus.UNAUTHORIZED);
    }
  }
}
