package com.ellu.looper.service;

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
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class FastApiService {

  private final WebClient webClient;

  public void sendNoteToAI(
      MeetingNoteRequest noteRequest,
      Consumer<MeetingNoteResponse> onSuccess,
      Consumer<Throwable> onError) {
    log.info("Sending note to AI server for project: {}", noteRequest.getAuthor_id());
    webClient
        .post()
        .uri("/ai/notes")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(noteRequest)
        .retrieve()
        .bodyToMono(MeetingNoteResponse.class)
        .timeout(Duration.ofMinutes(2)) // AI 서버와 통신 timeout
        .doOnSuccess(
            response -> {
              log.info(
                  "Successfully sent note to AI server for project: {}",
                  noteRequest.getAuthor_id());
            })
        .doOnError(
            error -> {
              log.error(
                  "Failed to send note to AI server for project: {}, error: {}",
                  noteRequest.getAuthor_id(),
                  error.getMessage());
            })
        .subscribe(onSuccess, onError);
  }

  public Mono<String> getTaskPreview(Long projectId) {
    log.info("Getting task preview from AI server for project: {}", projectId);
    return webClient
        .get()
        .uri("/projects/{projectId}/tasks/preview", projectId)
        .retrieve()
        .bodyToMono(String.class)
        .timeout(Duration.ofSeconds(10))
        .doOnSuccess(
            response -> {
              log.info("Successfully got task preview from AI server for project: {}", projectId);
            })
        .doOnError(
            error -> {
              log.error(
                  "Failed to get task preview from AI server for project: {}, error: {}",
                  projectId,
                  error.getMessage());
            });
  }

  public void createWiki(Long projectId, WikiRequest request) {
    log.info("Creating wiki for project: {}", projectId);
    webClient
        .post()
        .uri("/ai/wiki")
        .bodyValue(request)
        .retrieve()
        .bodyToMono(Void.class)
        .timeout(Duration.ofSeconds(10))
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

  public Mono<String> getWiki(Long projectId) {
    log.info("Getting wiki for project: {}", projectId);
    return webClient
        .get()
        .uri("/ai/wiki/{projectId}", projectId)
        .retrieve()
        .bodyToMono(String.class)
        .timeout(Duration.ofSeconds(10))
        .doOnSuccess(
            response -> {
              log.info("Successfully got wiki for project: {}", projectId);
            })
        .doOnError(
            error -> {
              log.error(
                  "Failed to get wiki for project: {}, error: {}", projectId, error.getMessage());
            });
  }

  public void updateWiki(Long projectId, WikiRequest request) {
    log.info("Updating wiki for project: {}", projectId);
    webClient
        .patch()
        .uri("/ai/wiki/{projectId}", projectId)
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
