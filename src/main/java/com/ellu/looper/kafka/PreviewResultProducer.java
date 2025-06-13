package com.ellu.looper.kafka;

import com.ellu.looper.fastapi.dto.MeetingNoteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PreviewResultProducer {

  private final KafkaTemplate<String, MeetingNoteResponse> kafkaTemplate;

  @Value("${kafka.topics.preview}")
  private String previewResultTopic;

  public void sendPreviewResult(Long projectId, MeetingNoteResponse response) {
    log.info("[PreviewResultProducer] Sending preview result for project: {}", projectId);
    kafkaTemplate.send(previewResultTopic, projectId.toString(), response);
    log.info("[PreviewResultProducer] Successfully sent preview result for project: {}", projectId);
  }
}
