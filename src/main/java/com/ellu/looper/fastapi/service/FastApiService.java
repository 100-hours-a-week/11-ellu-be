package com.ellu.looper.fastapi.service;

import com.ellu.looper.chat.dto.MessageRequest;
import com.ellu.looper.commons.PreviewHolder;
import com.ellu.looper.commons.enums.NotificationType;
import com.ellu.looper.fastapi.dto.MeetingNoteRequest;
import com.ellu.looper.fastapi.dto.MeetingNoteResponse;
import com.ellu.looper.fastapi.dto.WikiEmbeddingResponse;
import com.ellu.looper.notification.service.NotificationService;
import com.ellu.looper.project.dto.WikiRequest;
import com.ellu.looper.project.entity.Project;
import com.ellu.looper.project.entity.ProjectMember;
import com.ellu.looper.project.repository.ProjectMemberRepository;
import com.ellu.looper.project.repository.ProjectRepository;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class FastApiService {

  private final WebClient fastApiSummaryWebClient;
  private final WebClient fastApiChatbotWebClient;
  private final PreviewHolder previewHolder;
  private final NotificationService notificationService;
  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;

  public FastApiService(
      @Qualifier("fastApiSummaryWebClient") WebClient fastApiSummaryWebClient,
      @Qualifier("fastApiChatbotWebClient") WebClient fastApiChatbotWebClient,
      PreviewHolder previewHolder,
      NotificationService notificationService,
      ProjectRepository projectRepository,
      ProjectMemberRepository projectMemberRepository) {
    this.fastApiSummaryWebClient = fastApiSummaryWebClient;
    this.fastApiChatbotWebClient = fastApiChatbotWebClient;
    this.previewHolder = previewHolder;
    this.notificationService = notificationService;
    this.projectRepository = projectRepository;
    this.projectMemberRepository = projectMemberRepository;
  }

  public void handleWikiEmbeddingCompletion(
      Long projectId, WikiEmbeddingResponse wikiEmbeddingResponse) {
    log.info("[FastApiService] Handling wiki embedding response for project: {}", projectId);
    if (wikiEmbeddingResponse.getStatus().equals("embedding_done")) {
      Project project =
          projectRepository
              .findByIdAndDeletedAtIsNull(projectId)
              .orElseThrow(() -> new IllegalArgumentException("Project not found"));

      List<ProjectMember> members =
          projectMemberRepository.findByProjectAndDeletedAtIsNull(project);

      notificationService.sendProjectNotification(
          NotificationType.PROJECT_WIKI_READY, members, null, project);

    } else {
      log.warn("[FastApiService] Wiki embedding not completed yet.");
    }
    log.info(
        "[FastApiService] Successfully handled wiki completion response for project: {}",
        projectId);
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
    fastApiSummaryWebClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder.path("/projects/{projectId}/notes").build(noteRequest.getProject_id()))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(noteRequest)
        .retrieve()
        .bodyToMono(MeetingNoteResponse.class)
        .timeout(Duration.ofMinutes(10)) // AI 서버와 통신 timeout
        .subscribe(
            // 성공 시
            response -> {
              log.info("Successfully sent note to AI server for project: {}. Response will be handled by FastAPI callback.",
                  noteRequest.getProject_id());
            },
            // 에러 발생 시
            error -> {
              log.error(
                  "Failed to send note to AI server for project: {}, error: {}",
                  noteRequest.getProject_id(),
                  error.getMessage());
              if (onError != null) {
                onError.accept(error);
              }
            }
        );
  }

  public void createWiki(Long projectId, WikiRequest request) {
    log.info("Creating wiki for project: {}", projectId);
    fastApiSummaryWebClient
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
    fastApiSummaryWebClient
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

  public Flux<String> streamChatResponse(MessageRequest request) {
    return fastApiChatbotWebClient
        .post()
        .uri("/ai/chats")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.TEXT_EVENT_STREAM)
        .bodyValue(request)
        .retrieve()
        .bodyToFlux(String.class)
        .doOnError(error -> log.error("Error streaming chat response: {}", error.getMessage()));
  }
}
