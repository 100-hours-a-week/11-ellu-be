package com.ellu.looper.project.repository;

import com.ellu.looper.project.entity.Project;
import com.ellu.looper.project.entity.ProjectMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

  // 로그인한 사용자가 참여한 모든 프로젝트 멤버십 조회 (soft-delete 제외)
  List<ProjectMember> findByUserIdAndDeletedAtIsNull(Long userId);

  // 특정 프로젝트의 멤버들 조회 (soft-delete 제외)
  List<ProjectMember> findByProjectAndDeletedAtIsNull(Project project);

  Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

  List<ProjectMember> findByProjectIdAndPositionAndDeletedAtIsNull(Long projectId, String position);

  List<ProjectMember> findByProjectIdAndPosition(Long id, String position);

  boolean existsByProjectIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);
}
