package com.ellu.looper.project.controller;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.CurrentUser;
import com.ellu.looper.project.dto.CreatorExcludedProjectResponse;
import com.ellu.looper.project.dto.ProjectCreateRequest;
import com.ellu.looper.project.dto.ProjectResponse;
import com.ellu.looper.project.dto.ProjectUpdateRequest;
import com.ellu.looper.project.service.ProjectService;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequestMapping("/projects")
public class ProjectController {

  private final ProjectService projectService;
  private final WebClient webClient;
  private String aiServerUrl;

  public ProjectController(
      ProjectService projectService,
      @Qualifier("fastApiSummaryWebClient") WebClient webClient,
      @Value("${fastapi.summary-url}") String aiServerUrl) {
    this.projectService = projectService;
    this.webClient = webClient;
    this.aiServerUrl = aiServerUrl;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<?>> createProject(
      @CurrentUser Long userId, @RequestBody ProjectCreateRequest request) {
    projectService.createProject(request, userId);
    return ResponseEntity.ok(ApiResponse.success("project_created", null));
  }

  @PostMapping("/{projectId}/audio")
  public ResponseEntity<ApiResponse<?>> relayAudioToAI(
      @PathVariable Long projectId, @RequestParam("file") MultipartFile file) throws IOException {

    // File size limit: 10MB
    if (file.getSize() > 10 * 1024 * 1024) {
      return ResponseEntity.badRequest().body(ApiResponse.error("File size exceeds 10MB limit."));
    }
    // File type limit: mp3, mp4, wav
    String contentType = file.getContentType();
    if (!("audio/mpeg".equals(contentType)
        || "audio/mp4".equals(contentType)
        || "audio/wav".equals(contentType))) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Only mp3, mp4, and wav audio files are allowed."));
    }

    MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
    formData.add("audio_file", file.getResource());
    formData.add("project_id", projectId.toString());

    String aiResponse =
        webClient
            .post()
            .uri("ai/audio")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(formData))
            .retrieve()
            .bodyToMono(String.class)
            .block();
    return ResponseEntity.ok(ApiResponse.success("file_sent_to_ai", aiResponse));
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
}
