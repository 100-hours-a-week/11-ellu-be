package com.ellu.looper.service;

import com.ellu.looper.dto.UserResponse;
import com.ellu.looper.entity.User;
import com.ellu.looper.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public UserResponse getMyInfo(Long userId) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    return new UserResponse(
        user.getId(),
        user.getNickname(),
        user.getCreatedAt().toString(),
        user.getFileName() // S3 저장 주소로 수정해야 함.
        );
  }

  public void updateNickname(Long userId, String newNickname) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    user.updateNickname(newNickname);
    userRepository.save(user);
  }
}
