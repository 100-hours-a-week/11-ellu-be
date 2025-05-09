package com.ellu.looper.commons;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

@Slf4j
@Component
public class PreviewHolder {

  private final Map<Long, DeferredResult<ResponseEntity<?>>> waitingClients =
      new ConcurrentHashMap<>();

  public void register(Long projectId, DeferredResult<ResponseEntity<?>> result) {
    log.info("[PreviewHolder] Registering waiting client for project: {}", projectId);
    waitingClients.put(projectId, result);
  }

  public void remove(Long projectId) {
    log.info("[PreviewHolder] Removing waiting client for project: {}", projectId);
    waitingClients.remove(projectId);
  }

  public void complete(Long projectId, Object aiResponse) {
    log.info("[PreviewHolder] Completing response for project: {}", projectId);
    DeferredResult<ResponseEntity<?>> result = waitingClients.remove(projectId);
    if (result != null && !result.isSetOrExpired()) {
      log.info("[PreviewHolder] Setting result for project: {}", projectId);
      result.setResult(
          ResponseEntity.ok(Map.of("message", "schedule_fetched", "data", aiResponse)));
    } else {
      log.warn("[PreviewHolder] No waiting client found or result already set for project: {}", projectId);
    }
  }

  public void completeWithError(Long projectId, Throwable error) {
    log.error("[PreviewHolder] Completing with error for project: {}, error: {}", projectId, error.getMessage());
    DeferredResult<ResponseEntity<?>> result = waitingClients.remove(projectId);
    if (result != null) {
      result.setResult(
          ResponseEntity.status(500).body(
              Map.of("message", "internal_server_error", "data", null)
          ));
    }
  }

}
