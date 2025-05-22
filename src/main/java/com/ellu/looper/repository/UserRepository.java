package com.ellu.looper.repository;

import com.ellu.looper.dto.UserProjection;
import com.ellu.looper.entity.User;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  Optional<User> findByNickname(String nickname);

  Optional<User> findByNicknameAndDeletedAtIsNull(
      @NotBlank(message = "Nickname must not be empty") String nickname);

  @Query(value = "SELECT id, nickname, file_name FROM member WHERE nickname_choseong LIKE CONCAT(:prefix, '%') ORDER BY nickname LIMIT 10", nativeQuery = true)
  List<UserProjection> findTop10ByNicknameChoseongStartingWith(@Param("prefix") String prefix);

  @Query(value = """
      SELECT id, nickname, file_name 
      FROM member 
      WHERE nickname LIKE CONCAT(:query, '%') 
      OR nickname_choseong LIKE CONCAT(:query, '%') 

      ORDER BY nickname LIMIT 10
      """, nativeQuery = true)
  List<UserProjection> findTop10ByNicknameOrChoseongContaining(@Param("query") String query);

}
