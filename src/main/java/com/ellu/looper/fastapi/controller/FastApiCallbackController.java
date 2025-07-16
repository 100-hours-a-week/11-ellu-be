package com.ellu.looper.fastapi.controller;

import com.ellu.looper.fastapi.dto.WikiEmbeddingResponse;
import com.ellu.looper.fastapi.service.FastApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * AI 서버로부터 콜백을 받는 컨트롤러
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai-callback")
public class FastApiCallbackController {

  private final FastApiService aiCallbackService;

  @PostMapping("/wiki")
  public ResponseEntity<?> receiveWikiCompletion(
      @RequestBody WikiEmbeddingResponse wikiEmbeddingResponse) {
    Long projectId = wikiEmbeddingResponse.getProject_id();
    log.info(
        "[FastApiCallbackController] Received callback for project with id {} for wiki embedding completion",
        projectId);
    log.info("[FastApiCallbackController] Response status: {}", wikiEmbeddingResponse.getStatus());

    aiCallbackService.handleWikiEmbeddingCompletion(projectId, wikiEmbeddingResponse);

    log.info(
        "[FastApiCallbackController] Successfully processed wiki embedding completion callback for project with id {}",
        projectId);
    return ResponseEntity.ok().build(); // AI에게 200 OK 응답
  }
}
