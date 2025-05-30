package com.ellu.looper.service;

import com.ellu.looper.commons.PreviewHolder;
import com.ellu.looper.dto.MeetingNoteRequest;
import com.ellu.looper.dto.MeetingNoteResponse;
import com.ellu.looper.dto.WikiRequest;
import java.time.Duration;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class FastApiService {

  private final WebClient webClient;
  private final PreviewHolder previewHolder;

  // AI 서버로부터 응답을 전달받아 처리
  public void handleAiPreviewResponse(Long projectId, MeetingNoteResponse aiResponse) {
    log.info("[FastApiService] Handling AI preview response for project: {}", projectId);
    log.info("[FastApiService] Response message: {}", aiResponse.getMessage());

    if (aiResponse.getDetail() != null) {
      aiResponse
          .getDetail()
          .forEach(
              preview -> {
                log.info("[FastApiService] Keyword: {}", preview.getKeyword());
                log.info("[FastApiService] Subtasks: {}", preview.getSubtasks());
              });
    } else {
      log.warn("[FastApiService] No data received in the response");
    }

    // aiResponse는 AI 서버가 반환한 task preview 결과
    previewHolder.complete(projectId, aiResponse);

    log.info(
        "[FastApiService] Successfully handled AI preview response for project: {}", projectId);
  }

  // 예외 상황 처리
  public void handleAiPreviewError(Long projectId, Throwable error) {
    previewHolder.completeWithError(projectId, error);
  }

  public void sendNoteToAI(
      MeetingNoteRequest noteRequest,
      Consumer<MeetingNoteResponse> onSuccess,
      Consumer<Throwable> onError) {
    log.info("Sending note to AI server for project: {}", noteRequest.getProject_id());
    webClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder.path("/projects/{projectId}/notes").build(noteRequest.getProject_id()))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(noteRequest)
        .retrieve()
        .bodyToMono(MeetingNoteResponse.class)
        .timeout(Duration.ofMinutes(10)) // AI 서버와 통신 timeout
        .doOnSuccess(
            response -> {
              log.info(
                  "Successfully sent note to AI server for project: {}",
                  noteRequest.getProject_id());
            })
        .doOnError(
            error -> {
              log.error(
                  "Failed to send note to AI server for project: {}, error: {}",
                  noteRequest.getProject_id(),
                  error.getMessage());
            })
        .subscribe(onSuccess, onError);
  }

  public void createWiki(Long projectId, WikiRequest request) {
    log.info("Creating wiki for project: {}", projectId);
    webClient
        .post()
        .uri("/ai/wiki")
        .bodyValue(request)
        .retrieve()
        .bodyToMono(Void.class)
        .timeout(Duration.ofMinutes(10))
        .doOnSuccess(
            response -> {
              log.info("Successfully created wiki for project: {}", projectId);
            })
        .doOnError(
            error -> {
              log.error(
                  "Failed to create wiki for project: {}, error: {}",
                  projectId,
                  error.getMessage());
            })
        .subscribe();
  }

  public void updateWiki(Long projectId, WikiRequest request) {
    log.info("Updating wiki for project: {}", projectId);
    webClient
        .patch()
        .uri("/ai/wiki")
        .bodyValue(request)
        .retrieve()
        .bodyToMono(Void.class)
        .timeout(Duration.ofSeconds(10))
        .doOnSuccess(
            response -> {
              log.info("Successfully updated wiki for project: {}", projectId);
            })
        .doOnError(
            error -> {
              log.error(
                  "Failed to update wiki for project: {}, error: {}",
                  projectId,
                  error.getMessage());
            })
        .subscribe();
  }
}
