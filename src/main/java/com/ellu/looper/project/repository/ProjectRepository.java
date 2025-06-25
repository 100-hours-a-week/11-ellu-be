package com.ellu.looper.project.repository;

import com.ellu.looper.project.entity.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

  Optional<Project> findByIdAndDeletedAtIsNull(Long projectId);

  @Query(
      "SELECT pm.project FROM ProjectMember pm WHERE pm.user.id = :memberId AND pm.project.deletedAt IS NULL")
  List<Project> findAllByMemberId(@Param("memberId") Long memberId);
}
