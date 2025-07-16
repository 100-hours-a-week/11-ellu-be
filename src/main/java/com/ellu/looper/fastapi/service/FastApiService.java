package com.ellu.looper.fastapi.service;

import com.ellu.looper.chat.dto.MessageRequest;
import com.ellu.looper.commons.enums.NotificationType;
import com.ellu.looper.fastapi.dto.MeetingNoteRequest;
import com.ellu.looper.fastapi.dto.MeetingNoteResponse;
import com.ellu.looper.fastapi.dto.SchedulePreview;
import com.ellu.looper.fastapi.dto.TaskSelectionResultRequest;
import com.ellu.looper.fastapi.dto.WikiEmbeddingResponse;
import com.ellu.looper.notification.service.NotificationService;
import com.ellu.looper.project.dto.WikiRequest;
import com.ellu.looper.project.entity.Project;
import com.ellu.looper.project.entity.ProjectMember;
import com.ellu.looper.project.repository.ProjectMemberRepository;
import com.ellu.looper.project.repository.ProjectRepository;
import com.ellu.looper.schedule.dto.ProjectScheduleCreateRequest.ProjectScheduleDto;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
  private final NotificationService notificationService;
  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;

  public FastApiService(
      @Qualifier("fastApiSummaryWebClient") WebClient fastApiSummaryWebClient,
      @Qualifier("fastApiChatbotWebClient") WebClient fastApiChatbotWebClient,
      NotificationService notificationService,
      ProjectRepository projectRepository,
      ProjectMemberRepository projectMemberRepository) {
    this.fastApiSummaryWebClient = fastApiSummaryWebClient;
    this.fastApiChatbotWebClient = fastApiChatbotWebClient;
    this.notificationService = notificationService;
    this.projectRepository = projectRepository;
    this.projectMemberRepository = projectMemberRepository;
  }

  public void handleWikiEmbeddingCompletion(
      Long projectId, WikiEmbeddingResponse wikiEmbeddingResponse) {
    log.info("[FastApiService] Handling wiki embedding response for project: {}", projectId);
    if (wikiEmbeddingResponse.getStatus().equals("completed")) {
      Project project =
          projectRepository
              .findByIdAndDeletedAtIsNull(projectId)
              .orElseThrow(() -> new IllegalArgumentException("Project not found"));

      List<ProjectMember> members =
          projectMemberRepository.findByProjectAndDeletedAtIsNull(project);

      notificationService.sendProjectNotification(
          NotificationType.PROJECT_WIKI_READY, members, project.getMember().getId(), project);

    } else {
      log.warn("[FastApiService] Wiki embedding not completed yet.");
    }
    log.info(
        "[FastApiService] Successfully handled wiki completion response for project: {}",
        projectId);
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

  public void deleteWiki(Long projectId) {
    log.info("Deleting wiki for project: {}", projectId);
    fastApiSummaryWebClient
        .delete()
        .uri(uriBuilder -> uriBuilder.path("/projects/{projectId}/wiki").build(projectId))
        .retrieve()
        .bodyToMono(Void.class)
        .timeout(Duration.ofSeconds(10))
        .doOnSuccess(
            response -> {
              log.info("Successfully deleted wiki for project: {}", projectId);
            })
        .doOnError(
            error -> {
              log.error(
                  "Failed to delete wiki for project: {}, error: {}",
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

  public void sendSelectionResult(Long projectId, List<ProjectScheduleDto> projectSchedules) {

    Map<String, Map<String, Set<String>>> groupedSchedules =
        groupSchedulesByPositionAndTask(projectSchedules);
    List<SchedulePreview> content = convertGroupedSchedulesToPreviews(groupedSchedules);
    TaskSelectionResultRequest request = new TaskSelectionResultRequest(content);

    log.info("Sending task selection result to AI server for project {}", projectId);
    log.info("Request content: {}", content);

    sendRequestToFastApi(projectId, request);
  }

  private Map<String, Map<String, Set<String>>> groupSchedulesByPositionAndTask(
      List<ProjectScheduleDto> projectSchedules) {
    Map<String, Map<String, Set<String>>> grouped = new ConcurrentHashMap<>();

    projectSchedules.stream()
        .filter(this::isValidSchedule)
        .forEach(schedule -> addToGroupedSchedules(grouped, schedule));

    return grouped;
  }

  private boolean isValidSchedule(ProjectScheduleDto schedule) {
    return schedule.getPosition() != null
        && schedule.getTitle() != null
        && schedule.getDescription() != null;
  }

  private void addToGroupedSchedules(
      Map<String, Map<String, Set<String>>> grouped, ProjectScheduleDto schedule) {
    String position = schedule.getPosition();
    String task = schedule.getTitle();
    String subtask = schedule.getDescription();

    grouped
        .computeIfAbsent(position, k -> new ConcurrentHashMap<>())
        .computeIfAbsent(task, k -> ConcurrentHashMap.newKeySet())
        .add(subtask);
  }

  private List<SchedulePreview> convertGroupedSchedulesToPreviews(
      Map<String, Map<String, Set<String>>> groupedSchedules) {
    return groupedSchedules.entrySet().stream()
        .flatMap(
            positionEntry ->
                positionEntry.getValue().entrySet().stream()
                    .map(taskEntry -> createSchedulePreview(positionEntry.getKey(), taskEntry)))
        .toList();
  }

  private SchedulePreview createSchedulePreview(
      String position, Map.Entry<String, Set<String>> taskEntry) {
    return new SchedulePreview(position, taskEntry.getKey(), new ArrayList<>(taskEntry.getValue()));
  }

  private void sendRequestToFastApi(Long projectId, TaskSelectionResultRequest request) {
    fastApiSummaryWebClient
        .post()
        .uri(uriBuilder -> uriBuilder.path("/projects/{projectId}/insert").build(projectId))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(Void.class)
        .timeout(Duration.ofMinutes(10))
        .doOnSuccess(
            response -> log.info("Successfully sent selected result for project {}.", projectId))
        .doOnError(
            error ->
                log.error(
                    "Failed to send selected result for project {}, error: {}",
                    projectId,
                    error.getMessage()))
        .subscribe();
  }


  public MeetingNoteResponse sendNoteToAI(MeetingNoteRequest noteRequest) {
    log.info("Sending note to AI server for project: {}", noteRequest.getProject_id());
    return fastApiSummaryWebClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder.path("/projects/{projectId}/notes").build(noteRequest.getProject_id()))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(noteRequest)
        .retrieve()
        .bodyToMono(MeetingNoteResponse.class)
        .timeout(Duration.ofMinutes(10)) // AI 서버와 통신 timeout
        .block();
  }
}
