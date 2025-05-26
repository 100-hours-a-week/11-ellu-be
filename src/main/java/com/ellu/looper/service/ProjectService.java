package com.ellu.looper.service;

import com.ellu.looper.commons.enums.Color;
import com.ellu.looper.commons.enums.InviteStatus;
import com.ellu.looper.commons.enums.NotificationType;
import com.ellu.looper.commons.enums.Role;
import com.ellu.looper.dto.CreatorExcludedProjectResponse;
import com.ellu.looper.dto.MemberDto;
import com.ellu.looper.dto.ProjectCreateRequest;
import com.ellu.looper.dto.ProjectResponse;
import com.ellu.looper.dto.ProjectUpdateRequest;
import com.ellu.looper.dto.WikiRequest;
import com.ellu.looper.entity.Notification;
import com.ellu.looper.entity.NotificationTemplate;
import com.ellu.looper.entity.Project;
import com.ellu.looper.entity.ProjectMember;
import com.ellu.looper.entity.ProjectSchedule;
import com.ellu.looper.entity.User;
import com.ellu.looper.kafka.NotificationProducer;
import com.ellu.looper.kafka.dto.NotificationMessage;
import com.ellu.looper.repository.NotificationRepository;
import com.ellu.looper.repository.NotificationTemplateRepository;
import com.ellu.looper.repository.ProjectMemberRepository;
import com.ellu.looper.repository.ProjectRepository;
import com.ellu.looper.repository.ProjectScheduleRepository;
import com.ellu.looper.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final NotificationRepository notificationRepository;
  private final NotificationTemplateRepository notificationTemplateRepository;
  private final NotificationProducer notificationProducer;

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
      for (ProjectCreateRequest.AddedMember member : request.getAdded_members()) {
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
          WikiRequest.builder().content(request.getWiki()).project_id(project.getId())
              .updated_at(LocalDateTime.now()).build();
      fastApiService.createWiki(project.getId(), wikiRequest);
    }

    log.info("Sending invitation notification to project members");
    // 초대 알림 보내기
    sendInvitationNotification(addedUsers, creator, project);

    log.info("Project created successfully with ID: {}", project.getId());
  }

  private void sendInvitationNotification(List<User> addedUsers, User creator, Project project) {
    // Notification 생성
    NotificationTemplate inviteTemplate = notificationTemplateRepository
        .findByType(NotificationType.PROJECT_INVITED)
        .orElseThrow(() -> new IllegalArgumentException("초대 템플릿 없음"));

    Map<String, Object> payload = new HashMap<>();
    payload.put("creator", creator.getNickname());
    payload.put("project", project.getTitle());

    for (User user : addedUsers) {
      Notification notification = Notification.builder()
          .sender(creator)
          .receiver(user)
          .project(project)
          .template(inviteTemplate)
          .payload(payload)
          .isProcessed(false)
          .inviteStatus(String.valueOf(InviteStatus.PENDING))
          .createdAt(LocalDateTime.now())
          .build();
      notificationRepository.save(notification);

      // Kafka를 통해 알림 메시지 전송
      NotificationMessage message = new NotificationMessage(
          NotificationType.PROJECT_INVITED.toString(),
          project.getId(), creator.getId(), List.of(user.getId()), notificationService.renderTemplate(
          inviteTemplate.getTemplate(), notification));

      log.info("TRYING TO SEND KAFKA MESSAGE: {}", message.getMessage());
      notificationProducer.sendNotification(message);
    }
  }

  @Transactional(readOnly = true)
  public List<ProjectResponse> getProjects(Long userId) {
    log.info("Getting projects for user: {}", userId);
    List<ProjectMember> memberships =
        projectMemberRepository.findByUserIdAndDeletedAtIsNull(userId);

    // 중복 제거된 프로젝트만 추출
    List<Project> distinctProjects = memberships.stream()
        .map(ProjectMember::getProject)
        .filter(project -> project.getDeletedAt() == null)
        .distinct() // 중복 제거
        .collect(Collectors.toList());

    List<ProjectResponse> responses =
        distinctProjects.stream()
            .map(project -> {
              List<ProjectMember> members =
                  projectMemberRepository.findByProjectAndDeletedAtIsNull(project);
              List<MemberDto> memberDtos =
                  members.stream()
                      .map(pm ->
                          new MemberDto(
                              pm.getUser().getId(),
                              pm.getUser().getNickname(),
                              profileImageService.getProfileImageUrl(pm.getUser().getFileName()),
                              pm.getPosition()))
                      .collect(Collectors.toList());

              return new ProjectResponse(
                  project.getId(),
                  project.getTitle(),
                  project.getColor() != null ? project.getColor().name() : "E3EEFC",
                  memberDtos,
                  project.getWiki()
              );
            })
            .collect(Collectors.toList());

    log.info("Found {} projects for user: {}", responses.size(), userId);
    return responses;
  }

  @Transactional(readOnly = true)
  public CreatorExcludedProjectResponse getProjectDetail(Long projectId, Long userId) {
    log.info("Getting project details for project: {} and user: {}", projectId, userId);
    Project project =
        projectRepository
            .findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));

    if (!project.getMember().getId().equals(userId)) {
      throw new SecurityException("Only project creator can view this project");
    }

    List<ProjectMember> members = projectMemberRepository.findByProjectAndDeletedAtIsNull(project);

    // 생성자 추출 (ADMIN 역할이면서 userId와 일치하는 사용자)
    ProjectMember creator = members.stream()
        .filter(pm -> pm.getRole() == Role.ADMIN && pm.getUser().getId().equals(userId))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("프로젝트 생성자가 존재하지 않습니다."));

    // 생성자 제외한 멤버 리스트 생성
    List<MemberDto> memberDtos = members.stream()
        .filter(pm -> !pm.getUser().getId().equals(userId)) // 생성자 제외
        .map(pm ->
            new MemberDto(
                pm.getUser().getId(),
                pm.getUser().getNickname(),
                profileImageService.getProfileImageUrl(pm.getUser().getFileName()),
                pm.getPosition()))
        .collect(Collectors.toList());

    return new CreatorExcludedProjectResponse(
        project.getId(),
        project.getTitle(),
        project.getColor() != null ? project.getColor().name() : "E3EEFC",
        creator.getPosition(), // 생성자의 position만 포함
        memberDtos,
        project.getWiki());

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

    // TODO: Send deletion notification
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
      for (ProjectUpdateRequest.AddedMember member : request.getAdded_members()) {
        User user =
            userRepository.findByNicknameAndDeletedAtIsNull(member.getNickname())
                .orElseThrow(
                    () -> new IllegalArgumentException("User not found: " + member.getNickname()));
        updatedUsers.add(user);
        log.info("UPDATED" + updatedUsers);
        updatedPositions.put(user.getId(), member.getPosition());
      }
    }

    int totalMemberCount = updatedUsers.size() + 1;
    if (totalMemberCount > 8) {
      throw new IllegalArgumentException("Too many members (max 8 including creator)");
    }

    // 프로젝트 업데이트
    project = project.toBuilder()
        .title(request.getTitle() != null ? request.getTitle() : project.getTitle())
        .wiki(request.getWiki() != null ? request.getWiki() : project.getWiki())
        .color(request.getColor() != null ? Color.valueOf(request.getColor()) : project.getColor())
        .updatedAt(LocalDateTime.now())
        .build();
    projectRepository.save(project);

    // 멤버 업데이트

    // 기존 멤버 목록
    List<ProjectMember> existingMembers = projectMemberRepository.findByProjectAndDeletedAtIsNull(
        project);

    Optional<ProjectMember> creator = existingMembers.stream()
        .filter(pm -> pm.getUser().getId().equals(userId))
        .findFirst();
    // 포지션이 있는 경우 생성자의 포지션 수정
    if (creator.isPresent() && request.getPosition() != null) {
      ProjectMember creatorMember = creator.get();
      if (!request.getPosition().equals(creatorMember.getPosition())) {
        creatorMember.setPosition(request.getPosition());
        projectMemberRepository.save(creatorMember);
      }
    }

    // 요청에서 빠진 기존 멤버 삭제 처리
    List<ProjectMember> toRemove = existingMembers.stream()
        .filter(pm -> !pm.getUser().getId().equals(userId) &&
            updatedUsers.stream().noneMatch(u -> u.getId().equals(pm.getUser().getId())))
        .collect(Collectors.toList());
    toRemove.forEach(pm -> pm.setDeletedAt(LocalDateTime.now()));
    projectMemberRepository.saveAll(toRemove);
    // TODO: Send expulsion notification

    log.info("updatedUsers" + updatedUsers);
    // 새로운 멤버 추가 및 포지션 업데이트
    for (User user : updatedUsers) {
      Optional<ProjectMember> existing = existingMembers.stream()
          .filter(pm -> pm.getUser().getId().equals(user.getId()))
          .findFirst();

      String newPosition = updatedPositions.get(user.getId());

      if (existing.isPresent()) {
        ProjectMember existingMember = existing.get();
        if (!Objects.equals(existingMember.getPosition(), newPosition)) {
          existingMember.setPosition(newPosition);
          projectMemberRepository.save(existingMember);
        }
      } else {
        ProjectMember newMember = ProjectMember.builder()
            .project(project)
            .user(user)
            .position(newPosition)
            .role(Role.PARTICIPANT)
            .createdAt(LocalDateTime.now())
            .build();
        projectMemberRepository.save(newMember);
      }
    }

    // 위키 내용이 있다면 수정
    if (request.getWiki() != null && !request.getWiki().trim().isEmpty()) {
      log.info("Updating wiki for project: {}", projectId);
      WikiRequest wikiRequest =
          WikiRequest.builder().content(request.getWiki()).project_id(projectId)
              .updated_at(LocalDateTime.now()).build();
      fastApiService.createWiki(projectId, wikiRequest);
    }

    log.info("Project updated successfully: {}", projectId);
  }


  @Transactional
  public void createWiki(Long projectId, Long userId, WikiRequest request) {
    Project project =
        projectRepository
            .findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));

    if (!project.getMember().getId().equals(userId)) {
      throw new SecurityException("Only project creator can create wiki");
    }

    fastApiService.createWiki(projectId, request);
  }

  @Transactional
  public void updateWiki(Long projectId, Long userId, WikiRequest request) {
    Project project =
        projectRepository
            .findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));

    if (!project.getMember().getId().equals(userId)) {
      throw new SecurityException("Only project creator can modify wiki");
    }

    fastApiService.updateWiki(projectId, request);
  }
}
