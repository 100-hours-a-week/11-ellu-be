package com.ellu.looper.repository;

import com.ellu.looper.entity.Project;
import com.ellu.looper.entity.User;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

  List<Project> findByMember(User member);

  Optional<Project> findByIdAndDeletedAtIsNull(Long projectId);

  @Query("SELECT pm.project FROM ProjectMember pm WHERE pm.user.id = :memberId AND pm.project.deletedAt IS NULL")
  List<Project> findAllByMemberId(@Param("memberId") Long memberId);

}
