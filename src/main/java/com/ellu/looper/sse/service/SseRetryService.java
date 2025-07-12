package com.ellu.looper.sse.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseRetryService {

  private final SseService sseService;
  private final ConcurrentHashMap<String, RetryInfo> retryQueue = new ConcurrentHashMap<>();
  private final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(2);

  @Value("${sse.retry.max-attempts:3}")
  private int maxRetryAttempts;

  @Value("${sse.retry.initial-delay-ms:1000}")
  private long initialDelayMs;

  @Value("${sse.retry.backoff-multiplier:2}")
  private double backoffMultiplier;

  /** 메시지 전송 실패 시 재시도 큐에 추가 */
  public void scheduleRetry(String userId, String eventName, String data, String reason) {
    String retryKey = userId + ":" + eventName + ":" + System.currentTimeMillis();

    RetryInfo retryInfo =
        RetryInfo.builder()
            .userId(userId)
            .eventName(eventName)
            .data(data)
            .attempts(0)
            .reason(reason)
            .build();

    retryQueue.put(retryKey, retryInfo);

    // 첫 번째 재시도 스케줄링
    scheduleNextRetry(retryKey, retryInfo, initialDelayMs);

    log.debug("Scheduled retry for user {}: {} (reason: {})", userId, eventName, reason);
  }

  /** 다음 재시도 스케줄링 */
  private void scheduleNextRetry(String retryKey, RetryInfo retryInfo, long delayMs) {
    retryExecutor.schedule(
        () -> {
          try {
            retryInfo.incrementAttempts();

            // 재시도 실행
            boolean success =
                sseService.sendMessageWithRetry(
                    retryInfo.getUserId(),
                    retryInfo.getEventName(),
                    retryInfo.getData());

            if (success) {
              // 성공 시 큐에서 제거
              retryQueue.remove(retryKey);
              log.debug(
                  "Retry successful for user {}: {} (attempt {})",
                  retryInfo.getUserId(),
                  retryInfo.getEventName(),
                  retryInfo.getAttempts());
            } else if (retryInfo.getAttempts() < maxRetryAttempts) {
              // 실패 시 다음 재시도 스케줄링
              long nextDelay = (long) (delayMs * backoffMultiplier);
              scheduleNextRetry(retryKey, retryInfo, nextDelay);
              log.debug(
                  "Retry failed for user {}: {} (attempt {}/{})",
                  retryInfo.getUserId(),
                  retryInfo.getEventName(),
                  retryInfo.getAttempts(),
                  maxRetryAttempts);
            } else {
              // 최대 재시도 횟수 초과
              retryQueue.remove(retryKey);
              log.warn(
                  "Max retry attempts exceeded for user {}: {} (reason: {})",
                  retryInfo.getUserId(),
                  retryInfo.getEventName(),
                  retryInfo.getReason());
            }

          } catch (Exception e) {
            log.error("Error during retry for user {}: {}", retryInfo.getUserId(), e.getMessage());
            retryQueue.remove(retryKey);
          }
        },
        delayMs,
        TimeUnit.MILLISECONDS);
  }

  /** 현재 재시도 큐 상태 조회 */
  public int getRetryQueueSize() {
    return retryQueue.size();
  }

  /** 재시도 정보 클래스 */
  public static class RetryInfo {
    private String userId;
    private String eventName;
    private String data;
    private boolean done;
    private int attempts;
    private String reason;

    public RetryInfo() {}

    public static RetryInfoBuilder builder() {
      return new RetryInfoBuilder();
    }

    public void incrementAttempts() {
      this.attempts++;
    }

    // Getters and setters
    public String getUserId() {
      return userId;
    }

    public void setUserId(String userId) {
      this.userId = userId;
    }

    public String getEventName() {
      return eventName;
    }

    public void setEventName(String eventName) {
      this.eventName = eventName;
    }

    public String getData() {
      return data;
    }

    public void setData(String data) {
      this.data = data;
    }

    public boolean isDone() {
      return done;
    }

    public void setDone(boolean done) {
      this.done = done;
    }

    public int getAttempts() {
      return attempts;
    }

    public void setAttempts(int attempts) {
      this.attempts = attempts;
    }

    public String getReason() {
      return reason;
    }

    public void setReason(String reason) {
      this.reason = reason;
    }

    public static class RetryInfoBuilder {
      private RetryInfo retryInfo = new RetryInfo();

      public RetryInfoBuilder userId(String userId) {
        retryInfo.userId = userId;
        return this;
      }

      public RetryInfoBuilder eventName(String eventName) {
        retryInfo.eventName = eventName;
        return this;
      }

      public RetryInfoBuilder data(String data) {
        retryInfo.data = data;
        return this;
      }

      public RetryInfoBuilder done(boolean done) {
        retryInfo.done = done;
        return this;
      }

      public RetryInfoBuilder attempts(int attempts) {
        retryInfo.attempts = attempts;
        return this;
      }

      public RetryInfoBuilder reason(String reason) {
        retryInfo.reason = reason;
        return this;
      }

      public RetryInfo build() {
        return retryInfo;
      }
    }
  }
}
