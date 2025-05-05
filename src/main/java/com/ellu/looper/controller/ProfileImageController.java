package com.ellu.looper.controller;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.service.ProfileImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/profile-images")
@RequiredArgsConstructor
public class ProfileImageController {

    private final ProfileImageService profileImageService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> uploadProfileImage(
            @RequestParam("file") MultipartFile file) throws IOException {
        String imageKey = profileImageService.uploadProfileImage(file);
        return ResponseEntity.ok(ApiResponse.success("profile_image_uploaded", imageKey));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<String>>> listProfileImages() {
        List<String> imageKeys = profileImageService.listProfileImages();
        return ResponseEntity.ok(ApiResponse.success("profile_images_listed", imageKeys));
    }

    // 관리자용 API
    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse<Void>> initializeDefaultImages() throws IOException {
        profileImageService.uploadDefaultProfileImages();
        return ResponseEntity.ok(ApiResponse.success("profile_images_initialized", null));
    }
} 