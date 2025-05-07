package com.ellu.looper.commons;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

@Component
public class PreviewHolder {

  private final Map<Long, DeferredResult<ResponseEntity<?>>> waitingClients =
      new ConcurrentHashMap<>();

  public void register(Long projectId, DeferredResult<ResponseEntity<?>> result) {
    waitingClients.put(projectId, result);
  }

  public void remove(Long projectId) {
    waitingClients.remove(projectId);
  }

  public void complete(Long projectId, Object aiResponse) {
    DeferredResult<ResponseEntity<?>> result = waitingClients.remove(projectId);
    if (result != null && !result.isSetOrExpired()) {
      result.setResult(
          ResponseEntity.ok(Map.of("message", "schedule_fetched", "data", aiResponse)));
    }
  }

  public void completeWithError(Long projectId, Throwable error) {
    DeferredResult<ResponseEntity<?>> result = waitingClients.remove(projectId);
    if (result != null) {
      result.setResult(
          ResponseEntity.status(500).body(
              Map.of("message", "internal_server_error", "data", null)
          ));
    }
  }

}
