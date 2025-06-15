package com.ellu.looper.kafka;

import com.ellu.looper.commons.PreviewHolder;
import com.ellu.looper.fastapi.dto.MeetingNoteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PreviewResultConsumer {

  private final PreviewHolder previewHolder;

  @Value("${kafka.topics.preview}")
  private String previewResultTopic;

  @KafkaListener(topics = "${kafka.topics.preview}", groupId = "${kafka.consumer.preview-group-id}")
  public void consumePreviewResult(
      @Header(KafkaHeaders.RECEIVED_KEY) String projectId, @Payload MeetingNoteResponse response) {
    log.info("[PreviewResultConsumer] Received preview result for project: {}", projectId);
    previewHolder.complete(Long.parseLong(projectId), response);
    log.info(
        "[PreviewResultConsumer] Successfully processed preview result for project: {}", projectId);
  }
}
