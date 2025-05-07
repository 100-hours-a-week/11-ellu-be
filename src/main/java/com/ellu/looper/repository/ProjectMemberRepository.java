package com.ellu.looper.repository;

import com.ellu.looper.entity.Project;
import com.ellu.looper.entity.ProjectMember;
import com.ellu.looper.entity.User;
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

  // 특정 유저와 프로젝트로 멤버 조회 (unique 조건에 쓸 수 있음)
  Optional<ProjectMember> findByProjectAndUser(Project project, User member);

  // 특정 닉네임을 가진 유저의 멤버십 조회 (User.nickname 기준으로)
  List<ProjectMember> findByUser_NicknameAndDeletedAtIsNull(String nickname);
}
