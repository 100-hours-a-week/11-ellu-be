package com.ellu.looper.project.controller;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.CurrentUser;
import com.ellu.looper.commons.util.S3Service;
import com.ellu.looper.fastapi.dto.MeetingNoteRequest;
import com.ellu.looper.fastapi.dto.MeetingNoteResponse;
import com.ellu.looper.project.dto.CreatorExcludedProjectResponse;
import com.ellu.looper.project.dto.ProjectCreateRequest;
import com.ellu.looper.project.dto.ProjectResponse;
import com.ellu.looper.project.dto.ProjectUpdateRequest;
import com.ellu.looper.project.repository.ProjectMemberRepository;
import com.ellu.looper.project.service.ProjectService;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;


@Slf4j
@RestController
@RequestMapping("/projects")
public class ProjectController {

  private final ProjectService projectService;
  private final S3Service s3Service;
  private final WebClient webClient;
  private String aiServerUrl;
  private final ProjectMemberRepository projectMemberRepository;

  public ProjectController(
      ProjectService projectService, S3Service s3Service,
      @Qualifier("fastApiSummaryWebClient") WebClient webClient,
      @Value("${fastapi.summary-url}") String aiServerUrl,
      ProjectMemberRepository projectMemberRepository) {
    this.projectService = projectService;
    this.s3Service = s3Service;
    this.webClient = webClient;
    this.aiServerUrl = aiServerUrl;
    this.projectMemberRepository = projectMemberRepository;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<?>> createProject(
      @CurrentUser Long userId, @RequestBody ProjectCreateRequest request) {
    projectService.createProject(request, userId);
    return ResponseEntity.ok(ApiResponse.success("project_created", null));
  }

  @PostMapping("/{projectId}/audio")
  public ResponseEntity<?> relayAudioToAI(
      @PathVariable Long projectId, @RequestParam("file") MultipartFile file) throws IOException {

    // File size limit: 10MB
    if (file.getSize() > 10 * 1024 * 1024) {
      return ResponseEntity.badRequest().body(ApiResponse.error("File size exceeds 10MB limit."));
    }

    log.info("=== RECEIVED REQUEST ===");
    log.info("File name: {}", file.getOriginalFilename());
    log.info("File size: {}", file.getSize());
    log.info("Content type: {}", file.getContentType());
    
    // File type limit: mp3, mp4, wav
    String contentType = file.getContentType();
    if (!("audio/mpeg".equals(contentType)
        || "audio/mp4".equals(contentType)
        || "audio/wav".equals(contentType))) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Only mp3, mp4, and wav audio files are allowed."));
    }
    // S3에 파일 업로드
    String s3Url = s3Service.uploadAudioFile(file);
    log.info("Uploaded audio file to S3: {}", s3Url);

    // FastAPI에 S3 URL 전달
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("audio_file", s3Url);
    requestBody.put("project_id", projectId);

    log.info("Sending S3 audio URL to AI server...");
    log.info("Sending request to URI: {}{}", aiServerUrl, "/ai/audio");

    log.info("Sending request with Request body:{}",requestBody);
    try {
      String aiResponse = webClient
          .post()
          .uri("/ai/audio")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromFormData("audio_file", s3Url)
              .with("project_id", String.valueOf(projectId)))
          .retrieve()
          .bodyToMono(String.class)
          .block();

      log.info("=== FASTAPI RESPONSE ===");
      log.info("Response: {}", aiResponse);
      return ResponseEntity.ok(aiResponse);

    } catch (Exception e) {
      log.error("=== WEBCLIENT ERROR ===", e);
      throw e;
    }
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<ProjectResponse>>> getProjects(@CurrentUser Long userId) {
    List<ProjectResponse> responses = projectService.getProjects(userId);
    return ResponseEntity.ok(ApiResponse.success("project_list", responses));
  }

  @GetMapping("/{projectId}")
  public ResponseEntity<ApiResponse<CreatorExcludedProjectResponse>> getProjectDetails(
      @CurrentUser Long userId, @PathVariable Long projectId) {
    CreatorExcludedProjectResponse response = projectService.getProjectDetail(projectId, userId);
    return ResponseEntity.ok(ApiResponse.success("project_fetched", response));
  }

  @PatchMapping("/{projectId}")
  public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
      @CurrentUser Long userId,
      @PathVariable Long projectId,
      @RequestBody ProjectUpdateRequest request) {
    projectService.updateProject(projectId, request, userId);
    return ResponseEntity.ok(ApiResponse.success("project_updated", null));
  }

  @DeleteMapping("/{projectId}")
  public ResponseEntity<Void> deleteProject(
      @CurrentUser Long userId, @PathVariable Long projectId) {
    projectService.deleteProject(projectId, userId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{projectId}/notes")
  public ResponseEntity<?> createMeetingNote(
      @CurrentUser Long userId,
      @PathVariable Long projectId,
      @RequestBody MeetingNoteRequest request) {

    // 프로젝트 멤버십 확인
    projectMemberRepository
        .findByProjectIdAndUserIdAndDeletedAtIsNull(projectId, userId)
        .orElseThrow(() -> new AccessDeniedException("Not a member of this project"));

    if (request.getContent() == null || request.getContent().trim().isEmpty()) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Content must not be empty"));
    }
    MeetingNoteResponse response = projectService.sendNote(projectId, userId, request);

    if (response.getDetail() != null) {
      response
          .getDetail()
          .forEach(
              preview -> {
                log.info("[Meeting Note Result from FastApi] Task: {}", preview.getTask());
                log.info("[Meeting Note Result from FastApi] Subtasks: {}", preview.getSubtasks());
              });
    } else {
      log.warn("[Meeting Note Result from FastApi] No data received in the response");
    }

    return ResponseEntity.ok(response);
  }
}
