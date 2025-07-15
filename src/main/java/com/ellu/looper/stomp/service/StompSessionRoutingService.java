package com.ellu.looper.stomp.service;

import com.ellu.looper.stomp.dto.StompPubSubMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class StompSessionRoutingService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private static final String ROUTING_KEY_PREFIX = "stomp:routing:";
    private static final String STOMP_CHANNEL = "stomp:events";
    private static final long SESSION_TIMEOUT_HOURS = 1L;
    private static final String PROJECT_SESSION_SET_PREFIX = "project:calendar:";
    private final ObjectMapper objectMapper;
    private final Map<String, Boolean> localSessionMap = new ConcurrentHashMap<>();


    // 세션 등록
    public void registerSession(Long userId, String sessionId) {
        String key = ROUTING_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, sessionId, SESSION_TIMEOUT_HOURS, TimeUnit.HOURS);
        localSessionMap.put(sessionId, true);
        log.info("Registered STOMP session {} for user {}", sessionId, userId);
    }

    // 세션 해제
    public void unregisterSession(Long userId) {
        String key = ROUTING_KEY_PREFIX + userId;
        log.info("key!!"+ key);
        log.info("userId!! "+ userId);
        redisTemplate.delete(key);
        String sessionId = getSessionId(userId);
        if (sessionId != null) {
            localSessionMap.remove(sessionId);
        }
        log.info("Unregistered STOMP session for user {}", userId);
    }

    // 메시지 포워딩 (Pub/Sub)
    public void forwardToSession(Object message) {
        redisTemplate.convertAndSend(STOMP_CHANNEL, message);
        log.info("Published STOMP message to channel {}", STOMP_CHANNEL);
    }

    // 실제 세션에 메시지 전송
    public void sendToLocalSession(String sessionId, String eventName, String data, String projectId) {
        String destination = "/topic/" + projectId;
        messagingTemplate.convertAndSend(destination, data);
        log.info("[STOMP] Sent message to local session {}: event={}, data={}", sessionId, eventName, data);
    }

    // 프로젝트 단위 세션 등록
    public void registerProjectSession(Long projectId, String sessionId) {
        String key = PROJECT_SESSION_SET_PREFIX + projectId;
        redisTemplate.opsForSet().add(key, sessionId);
        // 세션 만료 관리(옵션): 일정 시간 후 자동 삭제
        redisTemplate.expire(key, SESSION_TIMEOUT_HOURS, TimeUnit.HOURS);
        log.info("Registered session {} to project {} set", sessionId, projectId);
    }

    // 프로젝트 단위 세션 해제
    public void unregisterProjectSession(Long projectId, String sessionId) {
        String key = PROJECT_SESSION_SET_PREFIX + projectId;
        redisTemplate.opsForSet().remove(key, sessionId);
        log.info("Unregistered session {} from project {} set", sessionId, projectId);
    }

    // 프로젝트 단위 세션 Set 조회
    public Set<String> getProjectSessionIds(Long projectId) {
        String key = PROJECT_SESSION_SET_PREFIX + projectId;
        Set<Object> rawSet = redisTemplate.opsForSet().members(key);
        if (rawSet == null) return Set.of();
        // Object -> String 변환
        return rawSet.stream().filter(String.class::isInstance).map(String.class::cast).collect(java.util.stream.Collectors.toSet());
    }

    // 세션 조회
    public String getSessionId(Long userId) {
        String key = ROUTING_KEY_PREFIX + userId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    // 현재 pod에 세션이 존재하는지 판별
    public boolean isCurrentSession(String sessionId) {
        return localSessionMap.containsKey(sessionId);
    }

    // 특정 sessionId에게 메시지 전송 (stateless 라우팅/포워딩)
    public void sendMessageToUser(String sessionId, String eventName, Object payload, String projectId) {
        String data;
        try {
            data = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize payload", e);
        }
        if (isCurrentSession(sessionId)) {
            // 현재 pod에 세션이 있으면 직접 전송
            sendToLocalSession(sessionId, eventName, data, projectId);
        } else {
            // Pub/Sub으로 포워딩
            StompPubSubMessage pubSubMessage = new StompPubSubMessage(sessionId, eventName, data, projectId);
            forwardToSession(pubSubMessage);
        }
    }

    // 모든 프로젝트 Set에서 해당 sessionId를 제거
    public void removeSessionFromAllProjects(String sessionId) {
        Set<String> keys = redisTemplate.keys(PROJECT_SESSION_SET_PREFIX + "*");
        if (keys != null) {
            for (String key : keys) {
                redisTemplate.opsForSet().remove(key, sessionId);
            }
        }
        log.info("Removed session {} from all project sets", sessionId);
    }
} 