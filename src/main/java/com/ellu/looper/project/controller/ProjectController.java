package com.ellu.looper.project.controller;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.CurrentUser;
import com.ellu.looper.project.dto.CreatorExcludedProjectResponse;
import com.ellu.looper.project.dto.ProjectCreateRequest;
import com.ellu.looper.project.dto.ProjectResponse;
import com.ellu.looper.project.dto.ProjectUpdateRequest;
import com.ellu.looper.project.service.ProjectService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/projects")
public class ProjectController {

  private final ProjectService projectService;

  @PostMapping
  public ResponseEntity<ApiResponse<?>> createProject(
      @CurrentUser Long userId, @RequestBody ProjectCreateRequest request) {
    projectService.createProject(request, userId);
    return ResponseEntity.ok(ApiResponse.success("project_created", null));
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
