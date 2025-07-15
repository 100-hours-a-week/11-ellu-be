package com.ellu.looper.kafka;

import com.ellu.looper.fastapi.dto.MeetingNoteResponse;
import com.ellu.looper.schedule.service.PreviewHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PreviewResultConsumer {

  private final PreviewHolder previewHolder;
  private final RedisTemplate<String, Object> redisTemplate;

  @Value("${kafka.topics.preview}")
  private String previewResultTopic;

  @KafkaListener(topics = "${kafka.topics.preview}", groupId = "${kafka.consumer.preview-group-id}")
  public void consumePreviewResult(
      @Header(KafkaHeaders.RECEIVED_KEY) String projectId, @Payload MeetingNoteResponse response) {
    // idempotent 처리: projectId + message 조합으로 Redis에 기록
    String uniqueKey = "preview:result:" + projectId + ":" + (response.getMessage() != null ? response.getMessage().hashCode() : "null");
    Boolean alreadyProcessed = redisTemplate.hasKey(uniqueKey);
    if (Boolean.TRUE.equals(alreadyProcessed)) {
      log.info("Duplicate preview result detected, skipping: {}", uniqueKey);
      return;
    }
    // 처리 완료 후 기록 (예: 10분 유지)
    redisTemplate.opsForValue().set(uniqueKey, "done", 10, TimeUnit.MINUTES);
    log.info("[PreviewResultConsumer] Received preview result for project: {}", projectId);
    previewHolder.complete(Long.parseLong(projectId), response);
    log.info(
        "[PreviewResultConsumer] Successfully processed preview result for project: {}", projectId);
  }
}
