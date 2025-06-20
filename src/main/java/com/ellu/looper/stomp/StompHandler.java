package com.ellu.looper.stomp;

import com.ellu.looper.exception.JwtException;
import com.ellu.looper.jwt.JwtAuthenticationToken;
import com.ellu.looper.jwt.JwtProvider;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StompHandler implements ChannelInterceptor {

  private final JwtProvider jwtProvider;

  public StompHandler(JwtProvider jwtProvider) {
    this.jwtProvider = jwtProvider;
  }

  // 인증 처리
  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

    if (StompCommand.CONNECT == accessor.getCommand()) {
      log.info("Validate token for stomp connect request");
      String bearerToken = accessor.getFirstNativeHeader("Authorization");
      if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
        throw new JwtException("Missing or malformed Authorization header", HttpStatus.BAD_REQUEST);
      }
      String token = bearerToken.substring(7);

      // 토큰 검증
      jwtProvider.validateToken(token);
      log.info("Successfully validated token for stomp connect request");

      Long userId = jwtProvider.extractUserId(token);

      // 인증 객체 생성
      Authentication auth = new JwtAuthenticationToken(userId); // 인증된 상태로 생성
      accessor.setUser(auth); // WebSocket 세션에 사용자 설정

      // 사용자 ID를 세션 속성에 저장 (Principal에서 추출)
      Object principal = auth.getPrincipal();
      if (!(principal instanceof Long)) {
        log.error(
            "Unexpected principal type: {}", principal == null ? "null" : principal.getClass());
        throw new JwtException(
            "Invalid authentication principal", HttpStatus.INTERNAL_SERVER_ERROR);
      }

      Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
      if (sessionAttributes == null) {
        log.error("WebSocket session attributes not initialized. Cannot store user ID.");
        throw new JwtException(
            "WebSocket session is not available", HttpStatus.INTERNAL_SERVER_ERROR);
      }
      sessionAttributes.put("userId", principal);
    }
    return message;
  }
}
