package com.ellu.looper.sse.service;

import com.ellu.looper.commons.PodInfo;
import com.ellu.looper.sse.dto.SseMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatSseService {

  private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final RedisTemplate<String, Object> redisTemplate;
  private final RestTemplate restTemplate;

  //  private final SseRetryService retryService;

  @Value("${sse.routing.chat.key-prefix}")
  private String routingKeyPrefix;

  @Value("${sse.routing.chat.ttl-hours}")
  private long routingTtlHours;

  @Value("${sse.internal.endpoint}")
  private String internalEndpoint;

  @Value("${sse.internal.key}")
  private String internalKey;

  @Value("${server.port}")
  private int serverPort;

  @Value("${server.ip}")
  private String podIp;

  private String podId;

  @PostConstruct
  public void initPodId() {
    this.podId = "POD-" + podIp + "-" + serverPort;
  }

  /** 채팅 스트림용 SSE 연결 */
  public SseEmitter createEmitter(String userId) {
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    emitters.put(userId, emitter);
    log.info("UserId {} is connected to chat stream. ", userId);

    emitter.onCompletion(
        () -> {
          log.info("SSE completed for user: {}", userId);
          emitters.remove(userId);
          unregisterUser(userId);
        });

    emitter.onTimeout(
        () -> {
          log.info("SSE timeout for user: {}", userId);
          emitters.remove(userId);
          unregisterUser(userId);
        });

    emitter.onError(
        e -> {
          log.error("SSE error for user: {}", userId, e);
          emitters.remove(userId);
          unregisterUser(userId);
        });

    registerUser(userId);
    return emitter;
  }

  /** 채팅 토큰 전송 */
  public void sendToken(String userId, String token, boolean done) {
    sendMessage(userId, "token", "{\"token\":\"" + token + "\",\"done\":" + done + "}");
  }

  /** 채팅 메시지 전송 */
  public void sendMessage(String userId, String message, boolean done) {
    sendMessage(userId, "message", "{\"message\":\"" + message + "\",\"done\":" + done + "}");
  }

  /** 스케줄 미리보기 전송 */
  public void sendSchedulePreview(
      String userId,
      String taskTitle,
      String category,
      String subtaskTitle,
      String startTime,
      String endTime,
      boolean done) {
    try {
      // JSON 객체 생성을 위해 ObjectMapper 사용
      ObjectNode rootNode = objectMapper.createObjectNode();
      rootNode.put("task_title", taskTitle);
      rootNode.put("category", category);

      ArrayNode schedulePreviewArray = objectMapper.createArrayNode();
      ObjectNode subtaskObject = objectMapper.createObjectNode();
      subtaskObject.put("title", subtaskTitle);
      subtaskObject.put("start_time", startTime);
      subtaskObject.put("end_time", endTime);
      schedulePreviewArray.add(subtaskObject);

      rootNode.set("schedule_preview", schedulePreviewArray);
      rootNode.put("done", done);

      sendMessage(userId, "schedule", rootNode.toString());
    } catch (Exception e) {
      log.error("Error creating schedule preview for user: {}", userId, e);
    }
  }

  /** 특정 사용자의 emitter 반환 */
  public SseEmitter getEmitter(String userId) {
    return emitters.get(userId);
  }

  /** 특정 사용자의 emitter 제거 */
  public void removeEmitter(String userId) {
    emitters.remove(userId);
  }

  /** Methods for Routing */

  /** 사용자 연결 시 라우팅 정보를 Redis에 저장 */
  public void registerUser(String userId) {
    PodInfo podInfo =
        PodInfo.builder()
            .podId(podId)
            .host(podIp)
            .port(serverPort)
            .connectedAt(System.currentTimeMillis())
            .build();

    String key = routingKeyPrefix + userId;
    redisTemplate.opsForValue().set(key, podInfo, routingTtlHours, TimeUnit.HOURS);
    log.info("Registered user {} to pod {}", userId, podId);
  }

  /** 사용자 연결 해제 시 라우팅 정보를 Redis에서 제거 */
  public void unregisterUser(String userId) {
    String key = routingKeyPrefix + userId;
    redisTemplate.delete(key);
    log.info("Unregistered user {} from pod {}", userId, podId);
  }

  /** 사용자에게 메시지 전송 (라우팅 포함) */
  public void sendMessage(String userId, String eventName, String data) {
    try {
      PodInfo targetPod = getTargetPod(userId);

      if (targetPod == null) {
        log.warn("No routing information found for user {}", userId);
        //        retryService.scheduleRetry(userId, eventName, data, "no_routing_info");
        return;
      }

      if (isCurrentPod(targetPod)) {
        // 현재 Pod에서 직접 전송
        sendToLocalUser(userId, eventName, data);
      } else {
        // 다른 Pod로 전달
        forwardToPod(targetPod, userId, eventName, data);
      }
    } catch (Exception e) {
      log.error("Error sending message to user {}: {}", userId, e.getMessage());
      //      retryService.scheduleRetry(userId, eventName, data, "send_error: " + e.getMessage());
    }
  }

  /** 재시도를 위한 메시지 전송 (재시도 서비스에서 호출) */
  public boolean sendMessageWithRetry(String userId, String eventName, String data) {
    try {
      PodInfo targetPod = getTargetPod(userId);

      if (targetPod == null) {
        log.debug("No routing information found for user {} during retry", userId);
        return false;
      }

      if (isCurrentPod(targetPod)) {
        // 현재 Pod에서 직접 전송
        sendToLocalUser(userId, eventName, data);
        return true;
      } else {
        // 다른 Pod로 전달
        return forwardToPodWithRetry(targetPod, userId, eventName, data);
      }
    } catch (Exception e) {
      log.error("Error during retry for user {}: {}", userId, e.getMessage());
      return false;
    }
  }

  /** 다른 Pod로 메시지 전달 */
  private void forwardToPod(PodInfo targetPod, String userId, String eventName, String data) {
    try {
      SseMessage message =
          SseMessage.builder().userId(userId).eventName(eventName).data(data).build();

      String url =
          String.format(
              "http://%s:%d%s", targetPod.getHost(), targetPod.getPort(), internalEndpoint);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("X-Internal-Key", internalKey);

      HttpEntity<SseMessage> request = new HttpEntity<>(message, headers);

      restTemplate.postForEntity(url, request, String.class);
      log.debug("Forwarded message to pod {} for user {}", targetPod.getPodId(), userId);

    } catch (Exception e) {
      log.error("Failed to forward message to pod {} for user {}", targetPod.getPodId(), userId, e);
      // 라우팅 정보가 잘못되었을 수 있으므로 제거
      unregisterUser(userId);
      throw e; // 재시도를 위해 예외를 다시 던짐
    }
  }

  /** 재시도를 위한 Pod 간 메시지 전달 */
  private boolean forwardToPodWithRetry(
      PodInfo targetPod, String userId, String eventName, String data) {
    try {
      SseMessage message =
          SseMessage.builder().userId(userId).eventName(eventName).data(data).build();

      String url =
          String.format(
              "http://%s:%d%s", targetPod.getHost(), targetPod.getPort(), internalEndpoint);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("X-Internal-Key", internalKey);

      HttpEntity<SseMessage> request = new HttpEntity<>(message, headers);

      restTemplate.postForEntity(url, request, String.class);
      log.debug("Forwarded message to pod {} for user {} (retry)", targetPod.getPodId(), userId);
      return true;

    } catch (Exception e) {
      log.error(
          "Failed to forward message to pod {} for user {} (retry)",
          targetPod.getPodId(),
          userId,
          e);
      return false;
    }
  }

  /** 로컬 사용자에게 메시지 전송 */
  public void sendToLocalUser(String userId, String eventName, String data) {
    try {
      SseEmitter emitter = getEmitter(userId);
      if (emitter != null) {
        emitter.send(SseEmitter.event().name(eventName).data(data));
        log.debug("Sent message to local user {}: {} - {}", userId, eventName, data);
        return;
      }

      log.warn("No emitter found for local user {}", userId);
      // emitter가 없으면 라우팅 정보도 제거
      unregisterUser(userId);
    } catch (Exception e) {
      log.error("Error sending message to local user {}", userId, e);
      // 전송 실패 시 emitter 제거 및 라우팅 정보 정리
      removeEmitter(userId);
      unregisterUser(userId);
    }
  }

  private String generatePodId() {
    return "pod-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
  }

  public String getCurrentPodId() {
    return podId;
  }

  /** 현재 Pod가 대상 Pod인지 확인 */
  private boolean isCurrentPod(PodInfo podInfo) {
    return podId.equals(podInfo.getPodId());
  }

  /** Redis에서 대상 Pod 정보 조회 */
  private PodInfo getTargetPod(String userId) {
    String key = routingKeyPrefix + userId;
    Object value = redisTemplate.opsForValue().get(key);

    if (value instanceof PodInfo) {
      return (PodInfo) value;
    }

    return null;
  }
}
