package com.ellu.looper.sse.scheduler;

import com.ellu.looper.sse.service.SseService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseRoutingCleanupScheduler {

  private final RedisTemplate<String, Object> redisTemplate;
  private final SseService sseService;

  @Value("${sse.routing.key-prefix:user:pod:}")
  private String routingKeyPrefix;

  /** 주기적으로 라우팅 정보를 정리 - 더 이상 존재하지 않는 emitter에 대한 라우팅 정보 제거 - 오래된 라우팅 정보 정리 */
  @Scheduled(fixedRateString = "${sse.routing.cleanup-interval-minutes:5}m")
  public void cleanupRoutingInfo() {
    try {
      log.debug("Starting SSE routing cleanup...");

      // Redis에서 모든 라우팅 키 조회
      Set<String> keys = redisTemplate.keys(routingKeyPrefix + "*");
      if (keys == null || keys.isEmpty()) {
        log.debug("No routing keys found for cleanup");
        return;
      }

      int cleanedCount = 0;
      for (String key : keys) {
        String userId = key.substring(routingKeyPrefix.length());

        // 해당 사용자의 emitter가 실제로 존재하는지 확인
        boolean hasEmitter = sseService.getEmitter(userId) != null;

        // emitter가 없으면 라우팅 정보 제거
        if (!hasEmitter) {
          redisTemplate.delete(key);
          cleanedCount++;
          log.debug("Cleaned up routing info for user {} (no active emitter)", userId);
        }
      }

      if (cleanedCount > 0) {
        log.info("Cleaned up {} stale routing entries", cleanedCount);
      } else {
        log.debug("No stale routing entries found");
      }

    } catch (Exception e) {
      log.error("Error during SSE routing cleanup", e);
    }
  }

  /** Pod 상태 확인 및 정리 - 현재 Pod가 관리하는 사용자들의 emitter 상태 확인 */
  @Scheduled(fixedRateString = "${sse.routing.health-check-interval-minutes:2}m")
  public void healthCheck() {
    try {
      String currentPodId = sseService.getCurrentPodId();
      log.debug("Performing health check for pod: {}", currentPodId);

      // 현재 Pod가 관리하는 사용자들의 emitter 상태 확인

    } catch (Exception e) {
      log.error("Error during SSE health check", e);
    }
  }
}
