package com.ellu.looper.user.repository;

import com.ellu.looper.user.dto.UserProjection;
import com.ellu.looper.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  Optional<User> findByNickname(String nickname);

  Optional<User> findByNicknameAndDeletedAtIsNull(
      @NotBlank(message = "Nickname must not be empty") String nickname);

  List<UserProjection> findTop10ByNicknameChoseongStartingWithOrderByNicknameAsc(String prefix);

  List<UserProjection>
      findTop10ByNicknameStartingWithIgnoreCaseOrNicknameChoseongStartingWithOrderByNicknameAsc(
          String nicknamePrefix, String choseongPrefix);
}
