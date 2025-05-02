package com.ellu.looper.controller;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.CurrentUser;
import com.ellu.looper.commons.PreviewHolder;
import com.ellu.looper.dto.MeetingNoteRequest;
import com.ellu.looper.service.FastApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MeetingNoteController {

    private final FastApiService fastApiService;
    private final PreviewHolder previewHolder;

    @PostMapping("/projects/{projectId}/notes")
    public ResponseEntity<ApiResponse<?>> createMeetingNote(
            @CurrentUser Long userId,
            @PathVariable Long projectId,
            @RequestBody MeetingNoteRequest request) {
        
        if (request.getMeetingNote() == null || request.getMeetingNote().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Content must not be empty")
            );
        }

        fastApiService.sendNoteToAI(request,
            aiResponse -> previewHolder.complete(projectId, aiResponse),
            error -> previewHolder.completeWithError(projectId, error)
        );

        return ResponseEntity.status(201).body(
            ApiResponse.success("note_uploaded", Map.of(
                "project_id", projectId,
                "author", Map.of(
                    "member_id", request.getAuthorId(),
                    "nickname", request.getNickname()
                ),
                "created_at", request.getCreatedAt()
            ))
        );
    }
} 