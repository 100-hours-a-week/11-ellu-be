package com.ellu.looper.repository;

import com.ellu.looper.entity.User;
import jakarta.validation.constraints.NotBlank;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  Optional<User> findByNickname(String nickname);

  Optional<User> findByNicknameAndDeletedAtIsNull(
      @NotBlank(message = "Nickname must not be empty") String nickname);
}
