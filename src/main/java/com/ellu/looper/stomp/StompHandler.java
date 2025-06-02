package com.ellu.looper.stomp;

import com.ellu.looper.jwt.JwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
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
      String token = bearerToken.substring(7);
      // 토큰 검증
      jwtProvider.validateToken(token);
      log.info("Successfully validated token for stomp connect request");
    }
    return message;
  }

}
