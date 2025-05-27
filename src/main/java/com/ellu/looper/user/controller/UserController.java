package com.ellu.looper.user.controller;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.CurrentUser;
import com.ellu.looper.user.dto.NicknameRequest;
import com.ellu.looper.user.dto.UserResponse;
import com.ellu.looper.user.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping("/me")
  public ResponseEntity<?> getMyInfo(@CurrentUser Long userId) {
    UserResponse userInfo = userService.getMyInfo(userId);
    return ResponseEntity.ok(ApiResponse.success("user_info", userInfo));
  }

  @PatchMapping("/me")
  public ResponseEntity<?> updateNickname(
      @CurrentUser Long userId, @RequestBody NicknameRequest request) {
    userService.updateNickname(userId, request.getNickname());
    return ResponseEntity.ok(ApiResponse.success("user_nickname_revised", null));
  }

  @GetMapping
  public ResponseEntity<?> searchNicknames(@CurrentUser Long userId, @RequestParam String query){
    List<UserResponse> userResponseList = userService.searchNicknames(query);
    return ResponseEntity.ok(ApiResponse.success("search_completed", userResponseList));
  }
}
