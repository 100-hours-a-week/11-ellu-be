package com.ellu.looper.admin;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.enums.NotificationType;
import com.ellu.looper.notification.entity.NotificationTemplate;
import com.ellu.looper.notification.repository.NotificationTemplateRepository;
import com.ellu.looper.user.service.ProfileImageService;
import com.ellu.looper.user.service.UserService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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


  private final NotificationTemplateRepository templateRepository;

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


  @PostMapping("/notification-template/initialize")
  public ResponseEntity<?> initializeTemplates() {
    Map<NotificationType, String> templates = Map.of(
        NotificationType.PROJECT_INVITED,
        "{creator}님이 {project} 프로젝트에 회원님을 {position}포지션으로 초대했습니다.",
        NotificationType.PROJECT_EXPELLED,
        "{project} 프로젝트에서 회원님을 내보냈습니다. 해당 프로젝트의 일정과 정보는 더 이상 보이지 않습니다.",
        NotificationType.PROJECT_DELETED,
        "{project} 프로젝트가 삭제되었습니다. 해당 프로젝트의 일정과 정보는 더 이상 보이지 않습니다.",
        NotificationType.SCHEDULE_CREATED, "{project}프로젝트에 새로운 {schedule}일정이 추가되었습니다.",
        NotificationType.SCHEDULE_UPDATED, "{project}프로젝트에 {schedule}일정이 업데이트되었습니다.",
        NotificationType.SCHEDULE_DELETED, "{project}프로젝트의 {schedule} 일정이 삭제되었습니다.",
        NotificationType.INVITATION_PROCESSED,
        "회원님이 {receiver}님께 보낸 {project} 프로젝트 초대 요청이 {status}되었습니다."
    );

    List<NotificationTemplate> savedTemplates = new ArrayList<>();

    for (Map.Entry<NotificationType, String> entry : templates.entrySet()) {
      if (!templateRepository.existsByType(entry.getKey())) {
        NotificationTemplate template = NotificationTemplate.builder()
            .type(entry.getKey())
            .template(entry.getValue())
            .build();
        savedTemplates.add(templateRepository.save(template));
      }
    }

    return ResponseEntity.ok(savedTemplates);
  }
}