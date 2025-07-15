package com.ellu.looper.stomp;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import com.ellu.looper.stomp.service.StompSessionRoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

/*
 * spring, stomp는 기본적으로 세션관리를 자동으로 내부적으로 처리함
 * 연결/해제 이벤트를 기록, 연결된 세션 수를 실시간으로 확인할 목적으로 이벤트 리스너를 생성 => 로그, 디버깅 목적
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StompEventListener {

  private final Set<String> sessions = ConcurrentHashMap.newKeySet();
  private final StompSessionRoutingService stompSessionRoutingService;

  @EventListener
  public void connectHandle(SessionConnectEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    sessions.add(accessor.getSessionId());
    log.info("Connect sessionId " + accessor.getSessionId());
    log.info("Total session : " + sessions.size());
    // userId 추출 및 Redis 등록
    Object userIdObj = accessor.getSessionAttributes() != null ? accessor.getSessionAttributes().get("userId") : null;
    if (userIdObj instanceof Long) {
      stompSessionRoutingService.registerSession((Long) userIdObj, accessor.getSessionId());
    } else {
      log.warn("userId not found in session attributes for sessionId {}", accessor.getSessionId());
    }
  }

  @EventListener
  public void disconnectHandle(SessionDisconnectEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    sessions.remove(accessor.getSessionId());
    log.info("Disconnect sessionId " + accessor.getSessionId());
    log.info("Total session : " + sessions.size());
    // 모든 프로젝트 Set에서 sessionId 제거
    stompSessionRoutingService.removeSessionFromAllProjects(accessor.getSessionId());
    // userId 추출 및 Redis 해제
    Object userIdObj = accessor.getSessionAttributes() != null ? accessor.getSessionAttributes().get("userId") : null;
    if (userIdObj instanceof Long) {
      stompSessionRoutingService.unregisterSession((Long) userIdObj);
    } else {
      log.warn("userId not found in session attributes for sessionId {} (disconnect)", accessor.getSessionId());
    }
  }

  @EventListener
  public void handleSubscribeEvent(SessionSubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String destination = accessor.getDestination(); // 예: /topic/project/123
    String sessionId = accessor.getSessionId();
    Long projectId = extractProjectIdFromDestination(destination);
    if (projectId != null && sessionId != null) {
      stompSessionRoutingService.registerProjectSession(projectId, sessionId);
      log.info("Registered session {} for project {} (subscribe)", sessionId, projectId);
    }
  }

  @EventListener
  public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String destination = accessor.getDestination();
    String sessionId = accessor.getSessionId();
    Long projectId = extractProjectIdFromDestination(destination);
    if (projectId != null && sessionId != null) {
      stompSessionRoutingService.unregisterProjectSession(projectId, sessionId);
      log.info("Unregistered session {} for project {} (unsubscribe)", sessionId, projectId);
    }
  }

  // destination에서 projectId 추출하는 유틸 함수
  private Long extractProjectIdFromDestination(String destination) {
    if (destination == null) return null;
    String[] parts = destination.split("/");
    try {
      return Long.valueOf(parts[parts.length - 1]);
    } catch (Exception e) {
      return null;
    }
  }
}
