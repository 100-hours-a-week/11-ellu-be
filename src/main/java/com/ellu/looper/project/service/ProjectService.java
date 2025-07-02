package com.ellu.looper.project.service;

import com.ellu.looper.commons.enums.Color;
import com.ellu.looper.commons.enums.NotificationType;
import com.ellu.looper.commons.enums.Role;
import com.ellu.looper.fastapi.service.FastApiService;
import com.ellu.looper.notification.service.NotificationService;
import com.ellu.looper.project.dto.AddedMember;
import com.ellu.looper.project.dto.CreatorExcludedProjectResponse;
import com.ellu.looper.project.dto.ProjectCreateRequest;
import com.ellu.looper.project.dto.ProjectResponse;
import com.ellu.looper.project.dto.ProjectUpdateRequest;
import com.ellu.looper.project.dto.WikiRequest;
import com.ellu.looper.project.entity.Project;
import com.ellu.looper.project.entity.ProjectMember;
import com.ellu.looper.project.repository.ProjectMemberRepository;
import com.ellu.looper.project.repository.ProjectRepository;
import com.ellu.looper.schedule.entity.Assignee;
import com.ellu.looper.schedule.entity.ProjectSchedule;
import com.ellu.looper.schedule.repository.AssigneeRepository;
import com.ellu.looper.schedule.repository.ProjectScheduleRepository;
import com.ellu.looper.user.dto.MemberDto;
import com.ellu.looper.user.entity.User;
import com.ellu.looper.user.repository.UserRepository;
import com.ellu.looper.user.service.ProfileImageService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final UserRepository userRepository;
  private final ProjectScheduleRepository projectScheduleRepository;
  private final FastApiService fastApiService;
  private final ProfileImageService profileImageService;
  private final NotificationService notificationService;
  private final AssigneeRepository assigneeRepository;
  private final RedisTemplate<String, Object> redisTemplate;
  private static final long PROJECT_CACHE_TTL_HOURS = 3L;
  private static final String PROJECT_DETAIL_CACHE_KEY_PREFIX = "project:";
  private static final String PROJECT_LIST_CACHE_KEY_PREFIX = "projects:user:";

  @Transactional
  public void createProject(ProjectCreateRequest request, Long creatorId) {
    log.info("Creating project with title: {} for user: {}", request.getTitle(), creatorId);

    User creator =
        userRepository
            .findById(creatorId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    Set<String> nicknameSet = new HashSet<>();
    List<User> addedUsers = new ArrayList<>();

    if (request.getAdded_members() != null) {
      for (AddedMember member : request.getAdded_members()) {
        String nickname = member.getNickname();

        // 생성자 본인을 초대하는 경우
        if (creator.getNickname().equals(nickname)) {
          throw new IllegalArgumentException("Cannot invite the project creator");
        }

        // 중복 닉네임 체크
        if (!nicknameSet.add(nickname)) {
          throw new IllegalArgumentException("Duplicate member: " + nickname);
        }

        User user =
            userRepository
                .findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + nickname));
        addedUsers.add(user);
      }
    }

    int totalMemberCount = addedUsers.size() + 1;
    if (totalMemberCount > 8) {
      throw new IllegalArgumentException("Too many members (max 8 including creator)");
    }

    if (totalMemberCount >= 2
        && (request.getPosition() == null || request.getPosition().isEmpty())) {
      throw new IllegalArgumentException("Creator's position is required");
    }

    Project project =
        new Project(
            null,
            creator,
            request.getTitle(),
            Color.valueOf(request.getColor()),
            LocalDateTime.now(),
            LocalDateTime.now(),
            null,
            request.getWiki());
    projectRepository.save(project);

    List<ProjectMember> projectMembers = new ArrayList<>();

    // 생성자 추가
    projectMembers.add(
        ProjectMember.builder()
            .project(project)
            .user(creator)
            .position(request.getPosition())
            .role(Role.ADMIN)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build());

    projectMemberRepository.saveAll(projectMembers);

    // wiki 저장하는 FastAPI 호출
    if (request.getWiki() != null && !request.getWiki().trim().isEmpty()) {
      log.info("Saving wiki in vectorDB for project: {}", project.getId());
      WikiRequest wikiRequest =
          WikiRequest.builder()
              .url(request.getWiki())
              .project_id(project.getId())
              .updated_at(LocalDateTime.now())
              .build();
      fastApiService.createWiki(project.getId(), wikiRequest);
    }

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronizationAdapter() {
          @Override
          public void afterCommit() {
            log.info("Sending invitation notification to project members");
            // 초대 알림 보내기
            notificationService.sendInvitationNotification(
                addedUsers, creator, project, request.getAdded_members());
          }
        });
    log.info("Project created successfully with ID: {}", project.getId());
    redisTemplate.delete(PROJECT_LIST_CACHE_KEY_PREFIX + creatorId);
  }

  @Transactional(readOnly = true)
  public List<ProjectResponse> getProjects(Long userId) {
    String cacheKey = PROJECT_LIST_CACHE_KEY_PREFIX + userId;
    List<ProjectResponse> cached =
        (List<ProjectResponse>) redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) {
      log.info("Cache hit for project list: {}", userId);
      return cached;
    }

    log.info("Cache miss for project list: {}", userId);

    // 사용자가 user인 ProjectMember + Project + User를 fetch join으로 한 번에 로딩
    List<ProjectMember> userProjectMembers =
        projectMemberRepository.findWithProjectAndUserByUserId(userId);

    // 중복 제거된 프로젝트 리스트 생성
    List<Project> distinctProjects =
        userProjectMembers.stream()
            .map(ProjectMember::getProject)
            .filter(project -> project.getDeletedAt() == null)
            .distinct()
            .collect(Collectors.toList());

    // 프로젝트 ID 리스트 추출
    List<Long> projectIds =
        distinctProjects.stream().map(Project::getId).collect(Collectors.toList());

    // 전체 프로젝트의 모든 멤버를 한 번에 fetch join으로 로딩
    List<ProjectMember> allProjectMembers =
        projectMemberRepository.findByProjectIdsWithUser(projectIds);

    // projectId로 그룹화
    Map<Long, List<ProjectMember>> projectMemberMap =
        allProjectMembers.stream().collect(Collectors.groupingBy(pm -> pm.getProject().getId()));

    List<ProjectResponse> responses =
        distinctProjects.stream()
            .map(
                project -> {
                  List<ProjectMember> members =
                      projectMemberMap.getOrDefault(project.getId(), List.of());

                  List<MemberDto> memberDtos =
                      members.stream()
                          .map(
                              pm ->
                                  new MemberDto(
                                      pm.getUser().getId(),
                                      pm.getUser().getNickname(),
                                      profileImageService.getProfileImageUrl(
                                          pm.getUser().getFileName()),
                                      pm.getPosition()))
                          .collect(Collectors.toList());

                  return new ProjectResponse(
                      project.getId(),
                      project.getTitle(),
                      project.getColor() != null ? project.getColor().name() : "E3EEFC",
                      memberDtos,
                      project.getWiki());
                })
            .collect(Collectors.toList());

    log.info("Found {} projects for user: {}", responses.size(), userId);
    redisTemplate.opsForValue().set(cacheKey, responses, PROJECT_CACHE_TTL_HOURS, TimeUnit.HOURS);
    return responses;
  }

  @Transactional(readOnly = true)
  public CreatorExcludedProjectResponse getProjectDetail(Long projectId, Long userId) {
    String cacheKey = PROJECT_DETAIL_CACHE_KEY_PREFIX + projectId;
    CreatorExcludedProjectResponse cached =
        (CreatorExcludedProjectResponse) redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) {
      log.info("Cache hit for project detail: {}", projectId);
      return cached;
    }
    log.info("Cache miss for project detail: {}. Loading from DB.", projectId);
    Project project =
        projectRepository
            .findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));

    if (!project.getMember().getId().equals(userId)) {
      CreatorExcludedProjectResponse response =
          new CreatorExcludedProjectResponse(
              project.getId(), project.getTitle(), project.getColor().name(), null, null, null);
      redisTemplate.opsForValue().set(cacheKey, response, PROJECT_CACHE_TTL_HOURS, TimeUnit.HOURS);
      return response;
    }

    List<ProjectMember> members = projectMemberRepository.findByProjectAndDeletedAtIsNull(project);
    ProjectMember creator =
        members.stream()
            .filter(pm -> pm.getRole() == Role.ADMIN && pm.getUser().getId().equals(userId))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("프로젝트 생성자가 존재하지 않습니다."));
    List<MemberDto> memberDtos =
        members.stream()
            .filter(pm -> !pm.getUser().getId().equals(userId))
            .map(
                pm ->
                    new MemberDto(
                        pm.getUser().getId(),
                        pm.getUser().getNickname(),
                        profileImageService.getProfileImageUrl(pm.getUser().getFileName()),
                        pm.getPosition()))
            .collect(Collectors.toList());
    CreatorExcludedProjectResponse response =
        new CreatorExcludedProjectResponse(
            project.getId(),
            project.getTitle(),
            project.getColor() != null ? project.getColor().name() : "E3EEFC",
            creator.getPosition(),
            memberDtos,
            project.getWiki());
    redisTemplate.opsForValue().set(cacheKey, response, PROJECT_CACHE_TTL_HOURS, TimeUnit.HOURS);
    return response;
  }

  @Transactional
  public void deleteProject(Long projectId, Long userId) {
    Project project =
        projectRepository
            .findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));

    if (!project.getMember().getId().equals(userId)) {
      throw new SecurityException("Only project creator can delete this project");
    }

    Project deltetedProject = project.toBuilder().deletedAt(LocalDateTime.now()).build();
    projectRepository.save(deltetedProject);

    // delete project schedule assignees
    List<Assignee> assignees =
        assigneeRepository.findByProjectIdThroughScheduleAndDeletedAtIsNull(projectId);
    for (Assignee assignee : assignees) {
      assignee.softDelete();
    }
    assigneeRepository.saveAll(assignees);

    // 프로젝트의 스케줄 삭제
    List<ProjectSchedule> schedules =
        projectScheduleRepository.findByProjectAndDeletedAtIsNull(project);
    for (ProjectSchedule schedule : schedules) {
      schedule.softDelete();
    }
    projectScheduleRepository.saveAll(schedules);

    // 프로젝트 멤버 삭제
    List<ProjectMember> members = projectMemberRepository.findByProjectAndDeletedAtIsNull(project);
    for (ProjectMember member : members) {
      member.setDeletedAt(LocalDateTime.now());
    }
    projectMemberRepository.saveAll(members);

    // send wiki deletion request to FastApi
    fastApiService.deleteWiki(projectId);

    // send deletion notification
    notificationService.sendProjectNotification(
        NotificationType.PROJECT_DELETED, members, userId, project);
    // 캐시 무효화
    redisTemplate.delete(PROJECT_DETAIL_CACHE_KEY_PREFIX + projectId);
    redisTemplate.delete(PROJECT_LIST_CACHE_KEY_PREFIX + userId);
  }

  @Transactional
  public void updateProject(Long projectId, ProjectUpdateRequest request, Long userId) {
    Project project =
        projectRepository
            .findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));

    if (!project.getMember().getId().equals(userId)) {
      throw new SecurityException("Only project creator can update this project");
    }

    if (request.getTitle() != null && request.getTitle().trim().isEmpty()) {
      throw new IllegalArgumentException("title must not be blank if provided");
    }

    // 닉네임이 잘못된 경우 예외 발생
    List<User> updatedUsers = new ArrayList<>();
    Map<Long, String> updatedPositions = new HashMap<>(); // userId -> position

    if (request.getAdded_members() != null) {
      for (AddedMember member : request.getAdded_members()) {
        User user =
            userRepository
                .findByNicknameAndDeletedAtIsNull(member.getNickname())
                .orElseThrow(
                    () -> new IllegalArgumentException("User not found: " + member.getNickname()));
        updatedUsers.add(user);
        updatedPositions.put(user.getId(), member.getPosition());
      }
    }

    int totalMemberCount = updatedUsers.size() + 1;
    if (totalMemberCount > 8) {
      throw new IllegalArgumentException("Too many members (max 8 including creator)");
    }

    // 프로젝트 업데이트
    project =
        project.toBuilder()
            .title(request.getTitle() != null ? request.getTitle() : project.getTitle())
            .wiki(request.getWiki() != null ? request.getWiki() : project.getWiki())
            .color(
                request.getColor() != null ? Color.valueOf(request.getColor()) : project.getColor())
            .updatedAt(LocalDateTime.now())
            .build();
    projectRepository.save(project);

    // 멤버 업데이트

    // 기존 멤버 목록
    List<ProjectMember> existingMembers =
        projectMemberRepository.findByProjectAndDeletedAtIsNull(project);

    ProjectMember creator =
        existingMembers.stream()
            .filter(pm -> pm.getUser().getId().equals(userId))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Project creator not found"));
    // 포지션이 있는 경우 생성자의 포지션 수정
    if (request.getPosition() != null) {
      if (!request.getPosition().equals(creator.getPosition())) {
        creator.setPosition(request.getPosition());
        projectMemberRepository.save(creator);
      }
    }

    // 요청에서 빠진 기존 멤버 삭제 처리
    List<ProjectMember> toRemove =
        existingMembers.stream()
            .filter(
                pm ->
                    !pm.getUser().getId().equals(userId)
                        && updatedUsers.stream()
                            .noneMatch(u -> u.getId().equals(pm.getUser().getId())))
            .collect(Collectors.toList());
    toRemove.forEach(pm -> pm.setDeletedAt(LocalDateTime.now()));
    projectMemberRepository.saveAll(toRemove);

    // send expulsion notification
    notificationService.sendProjectNotification(
        NotificationType.PROJECT_EXPELLED, toRemove, userId, project);

    // 새로운 멤버 추가 및 포지션 업데이트
    List<User> newlyInvitedUsers = new ArrayList<>();

    for (User user : updatedUsers) {
      Optional<ProjectMember> existing =
          existingMembers.stream()
              .filter(pm -> pm.getUser().getId().equals(user.getId()))
              .findFirst();

      String newPosition = updatedPositions.get(user.getId());

      if (existing.isPresent()) {
        ProjectMember existingMember = existing.get();
        if (!Objects.equals(existingMember.getPosition(), newPosition)) {
          existingMember.setPosition(newPosition);
          projectMemberRepository.save(existingMember);
        }
      } else { // newly invited members
        newlyInvitedUsers.add(user);
      }
    }
    // 초대 알림 보내기
    if (!newlyInvitedUsers.isEmpty()) {
      log.info("Sending invitation notification to newly invited project members");

      // AddedMember 객체로 변환
      List<AddedMember> newlyAddedMembers =
          request.getAdded_members().stream()
              .filter(
                  member ->
                      newlyInvitedUsers.stream()
                          .anyMatch(user -> user.getNickname().equals(member.getNickname())))
              .collect(Collectors.toList());

      notificationService.sendInvitationNotification(
          newlyInvitedUsers, creator.getUser(), project, newlyAddedMembers);
    }

    // 위키 내용이 있다면 수정
    if (request.getWiki() != null && !request.getWiki().trim().isEmpty()) {
      log.info("Updating wiki for project: {}", projectId);
      WikiRequest wikiRequest =
          WikiRequest.builder()
              .url(request.getWiki())
              .project_id(projectId)
              .updated_at(LocalDateTime.now())
              .build();
      fastApiService.createWiki(projectId, wikiRequest);
    }

    log.info("Project updated successfully: {}", projectId);
    // 캐시 무효화
    redisTemplate.delete(PROJECT_DETAIL_CACHE_KEY_PREFIX + projectId);
    redisTemplate.delete(PROJECT_LIST_CACHE_KEY_PREFIX + userId);
  }
}
