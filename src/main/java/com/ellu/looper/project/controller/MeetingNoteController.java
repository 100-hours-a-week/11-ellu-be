package com.ellu.looper.project.controller;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.CurrentUser;
import com.ellu.looper.fastapi.dto.MeetingNoteRequest;
import com.ellu.looper.fastapi.dto.MeetingNoteResponse;
import com.ellu.looper.fastapi.service.FastApiService;
import com.ellu.looper.project.entity.ProjectMember;
import com.ellu.looper.project.repository.ProjectMemberRepository;
import com.ellu.looper.user.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MeetingNoteController {

  private final FastApiService fastApiService;
  private final UserRepository userRepository;
  private final ProjectMemberRepository projectMemberRepository;

  @PostMapping("/projects/{projectId}/notes")
  public ResponseEntity<ApiResponse<?>> createMeetingNote(
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

    request.setProject_id(projectId);

    request.setNickname(userRepository.findById(userId).get().getNickname());

    // 프로젝트 멤버들의 모든 position을 가져옴
    List<String> positions =
        projectMemberRepository.findByProjectIdAndDeletedAtIsNull(projectId).stream()
            .map(ProjectMember::getPosition)
            .collect(Collectors.toList());
    request.setPosition(positions);

    // FastAPI에 동기 HTTP 요청
    MeetingNoteResponse response = fastApiService.sendNoteToAI(request);

    return ResponseEntity.ok(ApiResponse.success("preview_result", response));
  }
}
