package com.ellu.looper.config;

import com.ellu.looper.stomp.StompHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class StompWebsocketConfig implements WebSocketMessageBrokerConfigurer {

  private final StompHandler stompHandler;

  public StompWebsocketConfig(StompHandler stompHandler) {
    this.stompHandler = stompHandler;
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/connect")
        .setAllowedOrigins(
            "http://localhost:3000","http://localhost:3001",
            "https://looper.my",
            "https://dev.looper.my") // websocket관련 cors설정
        .withSockJS(); // ws://가 아닌 http:// endpoint를 사용할 수 있게 해주는 sockJs library를 통한 요청 허용
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    //  /app/*로 메시지가 발행되면 @Controller 객체의 @MessageMapping 매서드로 라우팅
    registry.setApplicationDestinationPrefixes("/app");
    //  /topic/* 형태로 메시지를 수신(subscribe)해야 함을 설정
    registry.enableSimpleBroker("/topic");
  }

  /*
  웹소켓 요청(connect, subscribe, disconnect) 시에는 http header에 http메시지를 넣어올 수 있고,
  이를 interceptor를 통해 가로채서 토큰 등을 검증할 수 있음
  */
  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(stompHandler);
  }
}
