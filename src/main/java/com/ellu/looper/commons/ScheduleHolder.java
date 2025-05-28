package com.ellu.looper.commons;

import com.ellu.looper.schedule.dto.ProjectScheduleCreateRequest;
import com.ellu.looper.schedule.dto.ProjectScheduleResponse;
import com.ellu.looper.schedule.service.ProjectScheduleService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleHolder {

  private final Map<String, DeferredResult<ResponseEntity<?>>> waitingClients = new ConcurrentHashMap<>();
  private final ProjectScheduleService scheduleService;

  public void register(Long projectId, Long userId, ProjectScheduleCreateRequest request,
      DeferredResult<ResponseEntity<?>> result) {
    String key = generateKey(projectId, userId);
    log.info("[ScheduleHolder] Registering dto creation request for key: {}", key);
    waitingClients.put(key, result);    // 비동기 처리 시작
    CompletableFuture.runAsync(() -> {
      try {
        log.info("[ScheduleHolder] Starting async dto creation for key: {}", key);
        List<ProjectScheduleResponse> schedules = scheduleService.createSchedules(projectId, userId,
            request);
        complete(projectId, userId, schedules);
      } catch (Exception e) {
        log.error("[ScheduleHolder] Error during async dto creation for key: {}, error: {}",
            key, e.getMessage(), e);
        completeWithError(projectId, userId, e);
      }
    });
  }

  public void complete(Long projectId, Long userId, List<ProjectScheduleResponse> response) {
    String key = generateKey(projectId, userId);
    log.info("[ScheduleHolder] Completing dto creation for key: {}", key);
    DeferredResult<ResponseEntity<?>> result = waitingClients.remove(key);
    if (result != null && !result.isSetOrExpired()) {
      log.info("[ScheduleHolder] Setting successful result for key: {}", key);
      result.setResult(ResponseEntity.ok(ApiResponse.success("project_daily_schedule", response)));
    } else {
      log.warn("[ScheduleHolder] No waiting client found or result already set for key: {}", key);
    }
  }

  public void completeWithError(Long projectId, Long userId, Throwable error) {
    String key = generateKey(projectId, userId);
    log.error("[ScheduleHolder] Completing with error for key: {}, error: {}", key,
        error.getMessage(), error);
    DeferredResult<ResponseEntity<?>> result = waitingClients.remove(key);
    if (result != null) {
      result.setResult(
          ResponseEntity.internalServerError().body(ApiResponse.error("internal_server_error")));
    }
  }

  public void remove(Long projectId, Long userId) {
    String key = generateKey(projectId, userId);
    log.info("[ScheduleHolder] Removing dto holder for key: {}", key);
    waitingClients.remove(key);
  }

  private String generateKey(Long projectId, Long userId) {
    return projectId + ":" + userId;
  }
}