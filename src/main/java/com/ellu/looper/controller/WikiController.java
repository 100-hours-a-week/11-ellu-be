package com.ellu.looper.controller;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.CurrentUser;
import com.ellu.looper.dto.WikiRequest;
import com.ellu.looper.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
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

        return ResponseEntity.status(201).body(
            ApiResponse.success("wiki_uploaded", Map.of("status", "success"))
        );
    }

    @GetMapping("/wiki")
    public ResponseEntity<ApiResponse<?>> getWiki(
            @CurrentUser Long userId,
            @PathVariable Long projectId) {

        String wiki = projectService.getWiki(projectId, userId);
        
        return ResponseEntity.ok(
            ApiResponse.success("wiki_fetched", Map.of(
                "wiki", wiki,
                "project_id", projectId
            ))
        );
    }

    @PatchMapping("/wiki")
    public ResponseEntity<ApiResponse<?>> updateWiki(
            @CurrentUser Long userId,
            @PathVariable Long projectId,
            @Valid @RequestBody WikiRequest request) {
        
        projectService.updateWiki(projectId, userId, request);

        return ResponseEntity.ok(
            ApiResponse.success("wiki_updated", Map.of(
                "wiki", request.getContent(),
                "project_id", projectId
            ))
        );
    }
} 