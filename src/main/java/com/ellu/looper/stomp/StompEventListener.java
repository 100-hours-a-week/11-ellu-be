package com.ellu.looper.stomp;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/*
 * spring, stomp는 기본적으로 세션관리를 자동으로 내부적으로 처리함
 * 연결/해제 이벤트를 기록, 연결된 세션 수를 실시간으로 확인할 목적으로 이벤트 리스너를 생성 => 로그, 디버깅 목적
 */
@Component
@Slf4j
public class StompEventListener {

  private final Set<String> sessions = ConcurrentHashMap.newKeySet();

  @EventListener
  public void connectHandle(SessionConnectEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    sessions.add(accessor.getSessionId());
    log.info("Connect sessionId " + accessor.getSessionId());
    log.info("Total session : " + sessions.size());
  }

  @EventListener
  public void disconnectHandle(SessionDisconnectEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    sessions.remove(accessor.getSessionId());
    log.info("Disconnect sessionId " + accessor.getSessionId());
    log.info("Total session : " + sessions.size());
  }
}
