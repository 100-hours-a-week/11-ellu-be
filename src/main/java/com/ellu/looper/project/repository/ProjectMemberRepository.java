package com.ellu.looper.project.repository;

import com.ellu.looper.project.entity.Project;
import com.ellu.looper.project.entity.ProjectMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

  // 사용자가 참여한 모든 프로젝트 멤버십 조회 (soft-delete 제외)
  @Query(
      "SELECT pm FROM ProjectMember pm "
          + "JOIN FETCH pm.project p "
          + "JOIN FETCH pm.user u "
          + "WHERE u.id = :userId AND pm.deletedAt IS NULL")
  List<ProjectMember> findWithProjectAndUserByUserId(@Param("userId") Long userId);

  // 프로젝트 id 목록에 있는 프로젝트들에 속한 모든 프로젝트 멤버 조회
  @Query(
      "SELECT pm FROM ProjectMember pm "
          + "JOIN FETCH pm.user u "
          + "WHERE pm.project.id IN :projectIds AND pm.deletedAt IS NULL")
  List<ProjectMember> findByProjectIdsWithUser(@Param("projectIds") List<Long> projectIds);

  // 특정 프로젝트의 멤버들 조회 (soft-delete 제외)
  List<ProjectMember> findByProjectAndDeletedAtIsNull(Project project);

  List<ProjectMember> findByProjectIdAndDeletedAtIsNull(Long projectId);

  // 프로젝트 id와 사용자 id로 프로젝트 멤버 조회 (soft-delete 제외)
  Optional<ProjectMember> findByProjectIdAndUserIdAndDeletedAtIsNull(Long projectId, Long userId);

  boolean existsByProjectIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

  // 프로젝트 id와 포지션(BE, FE, CLOUD, etc)으로 해당되는 프로젝트 멤버들 조회 (soft-delete 제외)
  List<ProjectMember> findByProjectIdAndPositionAndDeletedAtIsNull(Long projectId, String position);
}

