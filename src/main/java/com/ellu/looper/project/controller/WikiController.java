package com.ellu.looper.project.controller;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.CurrentUser;
import com.ellu.looper.project.dto.WikiRequest;
import com.ellu.looper.project.service.ProjectService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/*
 * 실제 서비스에서는 호출되지 않는 api들
 * 테스트 용도로 작성함
 */
@RestController
@RequestMapping("/projects/{projectId}")
@RequiredArgsConstructor
public class WikiController {

  private final ProjectService projectService;

  @PostMapping("/wiki")
  public ResponseEntity<ApiResponse<?>> createWiki(
      @CurrentUser Long userId,
      @PathVariable Long projectId,
      @Valid @RequestBody WikiRequest request) {

    projectService.createWiki(projectId, userId, request);

    return ResponseEntity.status(201)
        .body(ApiResponse.success("wiki_uploaded", Map.of("status", "success")));
  }

  @PatchMapping("/wiki")
  public ResponseEntity<ApiResponse<?>> updateWiki(
      @CurrentUser Long userId,
      @PathVariable Long projectId,
      @Valid @RequestBody WikiRequest request) {

    projectService.updateWiki(projectId, userId, request);

    return ResponseEntity.ok(
        ApiResponse.success(
            "wiki_updated", Map.of("wiki", request.getContent(), "project_id", projectId)));
  }
}
