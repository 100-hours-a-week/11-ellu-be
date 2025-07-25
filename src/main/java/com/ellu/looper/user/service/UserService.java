package com.ellu.looper.user.service;

import com.ellu.looper.commons.util.HangulUtil;
import com.ellu.looper.exception.NicknameAlreadyExistsException;
import com.ellu.looper.user.dto.UserProjection;
import com.ellu.looper.user.dto.UserResponse;
import com.ellu.looper.user.entity.User;
import com.ellu.looper.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.List;
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

    return new UserResponse(user.getId(), user.getNickname(), profileImageUrl);
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

  public List<UserResponse> searchNicknames(String query) {

    if (query.isEmpty()) {
      throw new RuntimeException("At least 1 character is required. ");
    }

    List<UserProjection> userProjections;

    if (HangulUtil.containsOnlyChoseong(query)) {
      // 순수 초성 검색: ㄱㄴㄷ 형태
      userProjections =
          userRepository.findTop10ByNicknameChoseongStartingWithOrderByNicknameAsc(query);
    } else {
      // 일반 검색 (부분 일치 포함, 대소문자 구분 없음)
      userProjections =
          userRepository
              .findTop10ByNicknameStartingWithIgnoreCaseOrNicknameChoseongStartingWithOrderByNicknameAsc(
                  query, query);
    }

    return userProjections.stream()
        .map(
            projection ->
                new UserResponse(
                    projection.getId(),
                    projection.getNickname(),
                    profileImageService.getProfileImageUrl(projection.getFileName())))
        .toList();
  }

  @Transactional
  public int updateNicknameChoseongForAllUsers() {
    List<User> users = userRepository.findAll();
    int updatedCount = 0;

    for (User user : users) {
      if (user.getNicknameChoseong() == null) {
        user.updateNickname(user.getNickname()); // updateNickname 메서드가 nickname_choseong도 업데이트함
        userRepository.save(user);
        updatedCount++;
      }
    }

    return updatedCount;
  }
}
