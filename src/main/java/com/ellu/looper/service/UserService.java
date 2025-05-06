package com.ellu.looper.service;

import com.ellu.looper.dto.UserResponse;
import com.ellu.looper.entity.User;
import com.ellu.looper.exception.NicknameAlreadyExistsException;
import com.ellu.looper.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final ProfileImageService profileImageService;

  public UserResponse getMyInfo(Long userId) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    String profileImageUrl = profileImageService.getProfileImageUrl(user.getFileName());

    return new UserResponse(
        user.getId(),
        user.getNickname(),
        profileImageUrl
    );
  }

  @Transactional
  public void updateNickname(Long userId, String newNickname) {
    if (newNickname.isEmpty() || newNickname.length() > 10) {
      throw new IllegalArgumentException("Nickname should be 1-10 letters.");
    }

    if (userRepository.findByNickname(newNickname).isPresent()) {
      throw new NicknameAlreadyExistsException("nickname_already_exists");
    }

    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    user.updateNickname(newNickname);
    userRepository.save(user);
  }
}
