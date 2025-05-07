package com.ellu.looper.controller;

import com.ellu.looper.dto.MeetingNoteResponse;
import com.ellu.looper.service.FastApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
* AI 서버로부터 콜백을 받는 컨트롤러
*/
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai-callback")
public class FastApiCallbackController {

  private final FastApiService aiCallbackService;

  @PostMapping("/projects/{projectId}/preview")
  public ResponseEntity<?> receiveAiPreview(
      @PathVariable Long projectId,
      @RequestBody MeetingNoteResponse aiPreviewResponse) {

    aiCallbackService.handleAiPreviewResponse(projectId, aiPreviewResponse);
    return ResponseEntity.ok().build(); // AI에게 200 OK 응답
  }
}

