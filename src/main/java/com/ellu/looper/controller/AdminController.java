package com.ellu.looper.controller;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.service.ProfileImageService;
import com.ellu.looper.service.UserService;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

  private final ProfileImageService profileImageService;

  private final UserService userService;

  @PostMapping("/nickname-choseong")
  public ResponseEntity<String> updateNicknameChoseong() {
    int updatedCount = userService.updateNicknameChoseongForAllUsers();
    return ResponseEntity.ok(String.format("Updated %d users' nickname_choseong", updatedCount));
  }

  @PostMapping("/profile-images")
  public ResponseEntity<ApiResponse<String>> uploadProfileImage(
      @RequestParam("file") MultipartFile file) throws IOException {
    String imageKey = profileImageService.uploadProfileImage(file);
    return ResponseEntity.ok(ApiResponse.success("profile_image_uploaded", imageKey));
  }

  @GetMapping("/profile-images")
  public ResponseEntity<ApiResponse<List<String>>> listProfileImages() {
    List<String> imageKeys = profileImageService.listProfileImages();
    return ResponseEntity.ok(ApiResponse.success("profile_images_listed", imageKeys));
  }

  // 관리자용 API
  @PostMapping("/profile-images/initialize")
  public ResponseEntity<ApiResponse<Void>> initializeDefaultImages() throws IOException {
    profileImageService.uploadDefaultProfileImages();
    return ResponseEntity.ok(ApiResponse.success("profile_images_initialized", null));
  }
}
