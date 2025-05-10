package com.ellu.looper.controller;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.CurrentUser;
import com.ellu.looper.dto.NicknameRequest;
import com.ellu.looper.dto.UserResponse;
import com.ellu.looper.service.UserService;
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
}
