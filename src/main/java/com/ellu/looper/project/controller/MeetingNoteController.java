package com.ellu.looper.project.controller;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.CurrentUser;
import com.ellu.looper.commons.PreviewHolder;
import com.ellu.looper.fastapi.service.FastApiService;
import com.ellu.looper.project.dto.MeetingNoteRequest;
import com.ellu.looper.project.entity.ProjectMember;
import com.ellu.looper.project.repository.ProjectMemberRepository;
import com.ellu.looper.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MeetingNoteController {

  private final FastApiService fastApiService;
  private final PreviewHolder previewHolder;
  private final UserRepository userRepository;
  private final ProjectMemberRepository projectMemberRepository;

  @PostMapping("/projects/{projectId}/notes")
  public ResponseEntity<ApiResponse<?>> createMeetingNote(
      @CurrentUser Long userId,
      @PathVariable Long projectId,
      @RequestBody MeetingNoteRequest request) {

    if (request.getContent() == null || request.getContent().trim().isEmpty()) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Content must not be empty"));
    }

    request.setProject_id(projectId);

    request.setNickname(userRepository.findById(userId).get().getNickname());

    ProjectMember member =
        projectMemberRepository
            .findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Project member not found"));
    request.setPosition(member.getPosition());

    fastApiService.sendNoteToAI(
        request,
        aiResponse -> previewHolder.complete(projectId, aiResponse),
        error -> previewHolder.completeWithError(projectId, error));

    return ResponseEntity.status(201)
        .body(
            ApiResponse.success(
                "note_uploaded",
                Map.of(
                    "project_id", projectId,
                    "author",
                        Map.of(
                            "member_id",
                            userId,
                            "nickname",
                            userRepository.findById(userId).get().getNickname()),
                    "created_at", LocalDateTime.now())));
  }
}
