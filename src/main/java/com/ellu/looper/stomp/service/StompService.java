package com.ellu.looper.stomp.service;

import com.ellu.looper.commons.PodInfo;
import com.ellu.looper.commons.enums.NotificationType;
import com.ellu.looper.kafka.ScheduleEventProducer;
import com.ellu.looper.kafka.dto.ScheduleEventMessage;
import com.ellu.looper.schedule.dto.ProjectScheduleCreateRequest;
import com.ellu.looper.schedule.dto.ProjectScheduleTakeRequest;
import com.ellu.looper.schedule.dto.StompProjectScheduleUpdateRequest;
import com.ellu.looper.stomp.dto.StompMessage;
import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequiredArgsConstructor
@Service
public class StompService {

  private final ScheduleEventProducer scheduleEventProducer;
  private final RedisTemplate<String, Object> redisTemplate;
  private final RestTemplate restTemplate;
  private final SimpMessagingTemplate messagingTemplate;

  @Value("${stomp.routing.ttl-hours}")
  private long routingTtlHours;

  @Value("${stomp.internal.endpoint}")
  private String internalEndpoint;

  @Value("${stomp.internal.key}")
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

  @Transactional
  public void updateSchedule(
      Long projectId, StompProjectScheduleUpdateRequest scheduleUpdateRequest, Long userId) {
    // Kafka schedule 토픽에 일정 변경 이벤트 발행 (다중 인스턴스 WebSocket 브로드캐스트용)
    ScheduleEventMessage updateEvent =
        ScheduleEventMessage.builder()
            .scheduleId(scheduleUpdateRequest.schedule_id())
            .userId(userId)
            .updateRequest(scheduleUpdateRequest)
            .projectId(String.valueOf(projectId))
            .type(NotificationType.SCHEDULE_UPDATED.name())
            .build();
    scheduleEventProducer.sendScheduleEvent(updateEvent);

    // WebSocket 전파는 Kafka Consumer가 담당하므로 이곳에서는 수행하지 않음

  }

  @Transactional
  public void deleteSchedule(Long projectId, Long scheduleId, Long userId) {
    ScheduleEventMessage deleteEvent =
        ScheduleEventMessage.builder()
            .scheduleId(scheduleId)
            .userId(userId)
            .projectId(projectId.toString())
            .type(NotificationType.SCHEDULE_DELETED.name())
            .schedule(ScheduleEventMessage.ScheduleDto.builder().id(scheduleId).build())
            .build();

    scheduleEventProducer.sendScheduleEvent(deleteEvent);
  }

  @Transactional
  public void createSchedule(
      Long projectId, ProjectScheduleCreateRequest createRequest, Long userId) {
    {
      ScheduleEventMessage event =
          ScheduleEventMessage.builder()
              .projectId(projectId.toString())
              .userId(userId)
              .createRequest(createRequest)
              .type(NotificationType.SCHEDULE_CREATED.name())
              .build();

      scheduleEventProducer.sendScheduleEvent(event);
    }
  }

  @Transactional
  public void takeSchedule(Long projectId, ProjectScheduleTakeRequest takeRequest, Long userId) {
    // Kafka로 스케줄 업데이트 이벤트 발행
    ScheduleEventMessage event =
        ScheduleEventMessage.builder()
            .projectId(projectId.toString())
            .scheduleId(takeRequest.schedule_id())
            .userId(userId)
            .type(NotificationType.SCHEDULE_TAKEN.name())
            .build();

    scheduleEventProducer.sendScheduleEvent(event);
  }

  public void sendMessage(String destination, Object payload) {
    try {
      String projectId = extractProjectIdFromDestination(destination);
      if (projectId == null) {
        log.warn("Could not extract projectId from destination: {}", destination);
        return;
      }

      // 로컬 pod의 구독자들에게 브로드캐스트
      sendToLocalUser(destination, payload);

      // 해당 프로젝트에 연결된 다른 pod들에도 브로드캐스트 전송
      broadcastToOtherPods(projectId, destination, payload);
    } catch (Exception e) {
      log.error("Error sending stomp message to destination {}: {}", destination, e.getMessage());
    }
  }

  public void sendToLocalUser(String destination, Object payload) {
    try {
      messagingTemplate.convertAndSend(destination, payload);
      log.info("Broadcasted stomp message to destination {} for team members", destination);
    } catch (Exception e) {
      log.error("Error broadcasting stomp message to destination {}", destination, e);
    }
  }

  private void broadcastToOtherPods(String projectId, String destination, Object payload) {
    try {
      // Redis에서 해당 프로젝트에 연결된 모든 pod 정보를 조회
      String projectKey = "stomp:" + projectId + ":pods";
      Set<Object> podInfos = redisTemplate.opsForSet().members(projectKey);

      if (podInfos == null || podInfos.isEmpty()) {
        log.debug("No pods found for project {}", projectId);
        return;
      }

      log.debug("Found {} total pods for project {}: {}", podInfos.size(), projectId, podInfos);

      Set<PodInfo> targetPods = new HashSet<>();

      for (Object podInfoObj : podInfos) {
        if (podInfoObj instanceof PodInfo) {
          PodInfo podInfo = (PodInfo) podInfoObj;
          log.debug(
              "Processing pod: {} ({}:{})",
              podInfo.getPodId(),
              podInfo.getHost(),
              podInfo.getPort());
          // 현재 pod가 아닌 다른 pod들만 대상으로 함
          if (!podInfo.getPodId().equals(this.podId)) {
            targetPods.add(podInfo);
            log.debug(
                "Added target pod: {} ({}:{})",
                podInfo.getPodId(),
                podInfo.getHost(),
                podInfo.getPort());
          } else {
            log.debug("Skipping current pod: {}", this.podId);
          }
        } else {
          log.warn("Unexpected pod info type: {}", podInfoObj.getClass().getName());
        }
      }

      log.info(
          "Found {} other pods for project {} (current pod: {})",
          targetPods.size(),
          projectId,
          this.podId);

      // 각 pod에 브로드캐스트 전송
      for (PodInfo targetPod : targetPods) {
        log.debug(
            "Forwarding to pod: {} ({}:{})",
            targetPod.getPodId(),
            targetPod.getHost(),
            targetPod.getPort());
        forwardToPod(targetPod, destination, payload);
      }

      if (!targetPods.isEmpty()) {
        log.info(
            "Broadcasted to {} other pods for project {} for destination {}",
            targetPods.size(),
            projectId,
            destination);
      }
    } catch (Exception e) {
      log.error(
          "Error broadcasting to project pods for project {} for destination {}",
          projectId,
          destination,
          e);
    }
  }

  private void forwardToPod(PodInfo targetPod, String destination, Object payload) {
    try {
      StompMessage message =
          StompMessage.builder().destination(destination).payload(payload).build();
      String url =
          String.format(
              "http://%s:%d%s", targetPod.getHost(), targetPod.getPort(), internalEndpoint);
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("X-Internal-Key", internalKey);
      HttpEntity<StompMessage> request = new HttpEntity<>(message, headers);
      restTemplate.postForEntity(url, request, String.class);
      log.debug(
          "Forwarded broadcast message to pod {} for destination {}",
          targetPod.getPodId(),
          destination);

    } catch (Exception e) {
      log.error(
          "Failed to forward broadcast message to pod {} for destination {}",
          targetPod.getPodId(),
          destination,
          e);
      throw e;
    }
  }

  private String generatePodId() {
    return "pod-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
  }

  private String extractProjectIdFromDestination(String destination) {
    // "/topic/{projectId}" 형태에서 projectId 추출
    if (destination != null && destination.startsWith("/topic/")) {
      return destination.substring("/topic/".length());
    }
    return null;
  }

  /** 프로젝트 토픽 구독 시 현재 pod를 해당 프로젝트의 Pod 목록에 등록 */
  public void registerPodToProject(String projectId) {
    try {
      String projectKey = "stomp:" + projectId + ":pods";

      // Pod 정보를 JSON 형태로 저장
      PodInfo podInfo =
          PodInfo.builder()
              .podId(podId)
              .host(podIp)
              .port(serverPort)
              .connectedAt(System.currentTimeMillis())
              .build();

      redisTemplate.opsForSet().add(projectKey, podInfo);
      redisTemplate.expire(projectKey, java.time.Duration.ofHours(routingTtlHours));
      log.info("Registered pod {} ({}:{}) to project {}", podId, podIp, serverPort, projectId);
    } catch (Exception e) {
      log.error("Error registering pod {} to project {}", podId, projectId, e);
    }
  }

  /** 프로젝트 토픽 구독 해제 시 현재 pod를 해당 프로젝트의 Pod 목록에서 제거 */
  public void unregisterPodFromProject(String projectId) {
    try {
      String projectKey = "stomp:" + projectId + ":pods";

      // Pod 정보를 JSON 형태로 제거
      PodInfo podInfo =
          PodInfo.builder()
              .podId(podId)
              .host(podIp)
              .port(serverPort)
              .connectedAt(System.currentTimeMillis())
              .build();

      redisTemplate.opsForSet().remove(projectKey, podInfo);
      log.debug("Unregistered pod {} from project {}", podId, projectId);
    } catch (Exception e) {
      log.error("Error unregistering pod {} from project {}", podId, projectId, e);
    }
  }
}
