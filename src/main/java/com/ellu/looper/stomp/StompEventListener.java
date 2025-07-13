package com.ellu.looper.stomp;

import com.ellu.looper.stomp.service.StompService;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

/*
 * spring, stomp는 기본적으로 세션관리를 자동으로 내부적으로 처리함
 * 연결/해제 이벤트를 기록, 연결된 세션 수를 실시간으로 확인할 목적으로 이벤트 리스너를 생성 => 로그, 디버깅 목적
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompEventListener {

  private final Set<String> sessions = ConcurrentHashMap.newKeySet();
  private final Map<String, Long> sessionIdToUserId = new ConcurrentHashMap<>();
  private final StompService stompService;

  private final String podId = generatePodId();

  @EventListener
  public void connectHandle(SessionConnectEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    sessions.add(accessor.getSessionId());
    // userId 추출
    Long userId = (Long) accessor.getSessionAttributes().get("userId");
    if (userId != null) {
      sessionIdToUserId.put(accessor.getSessionId(), userId);
      log.info("User {} connected to pod {} (stomp)", userId, podId);
    }
    log.info("Connect sessionId " + accessor.getSessionId());
    log.info("Total session : " + sessions.size());
  }

  @EventListener
  public void disconnectHandle(SessionDisconnectEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    sessions.remove(accessor.getSessionId());
    Long userId = sessionIdToUserId.remove(accessor.getSessionId());
    if (userId != null) {
      log.info("User {} disconnected from pod {} (stomp)", userId, podId);
    }
    log.info("Disconnect sessionId " + accessor.getSessionId());
    log.info("Total session : " + sessions.size());
  }

  @EventListener
  public void subscribeHandle(SessionSubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String destination = accessor.getDestination();

    if (destination != null && destination.startsWith("/topic/")) {
      String projectId = destination.substring("/topic/".length());
      stompService.registerPodToProject(projectId);
      log.info("Subscribed to project {} on pod {} (stomp)", projectId, podId);
    }
  }

  @EventListener
  public void unsubscribeHandle(SessionUnsubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String destination = accessor.getDestination();

    if (destination != null && destination.startsWith("/topic/")) {
      String projectId = destination.substring("/topic/".length());
      stompService.unregisterPodFromProject(projectId);
      log.info("Unsubscribed from project {} on pod {} (stomp)", projectId, podId);
    }
  }

  private String generatePodId() {
    return "pod-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
  }
}
