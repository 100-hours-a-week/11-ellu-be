package com.ellu.looper.service;

import com.ellu.looper.commons.enums.Role;
import com.ellu.looper.dto.MemberDto;
import com.ellu.looper.dto.ProjectCreateRequest;
import com.ellu.looper.dto.ProjectResponse;
import com.ellu.looper.entity.Project;
import com.ellu.looper.entity.ProjectMember;
import com.ellu.looper.entity.User;
import com.ellu.looper.repository.ProjectMemberRepository;
import com.ellu.looper.repository.ProjectRepository;
import com.ellu.looper.repository.UserRepository;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final AiService aiService;

    @Transactional
    public void createProject(ProjectCreateRequest request, Long creatorId) {

        User creator = userRepository.findById(creatorId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<User> addedUsers = new ArrayList<>();
        if (request.getAddedMembers() != null) {
            for (ProjectCreateRequest.AddedMember member : request.getAddedMembers()) {
                User user = userRepository.findByNickname(member.getNickname())
                    .orElseThrow(() -> new IllegalArgumentException(
                        "User not found: " + member.getNickname()));
                addedUsers.add(user);
            }
        }

        int totalMemberCount = addedUsers.size() + 1;
        if (totalMemberCount > 8) {
            throw new IllegalArgumentException("Too many members (max 8 including creator)");
        }

        if (totalMemberCount >= 2 && (request.getPosition() == null || request.getPosition()
            .isEmpty())) {
            throw new IllegalArgumentException(
                "Creator's position is required");
        }

        Project project = new Project(
            null,
            creator,
            request.getTitle(),
            null, // color enum 매칭은 version2+
            LocalDateTime.now(),
            LocalDateTime.now(),
            null
        );
        projectRepository.save(project);

        List<ProjectMember> projectMembers = new ArrayList<>();

        // 생성자 추가
        projectMembers.add(ProjectMember.builder()
            .project(project)
            .user(creator)
            .position(request.getPosition())
            .role(Role.ADMIN)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build()
        );

        // 초대 멤버 추가
        for (int i = 0; i < addedUsers.size(); i++) {
            projectMembers.add(ProjectMember.builder()
                .project(project)
                .user(addedUsers.get(i))
                .position(request.getAddedMembers().get(i).getPosition())
                .role(Role.PARTICIPANT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()
            );
        }

        projectMemberRepository.saveAll(projectMembers);

        // TODO: 초대 알림 보내기 (version2+)
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjects(Long userId) {
        List<ProjectMember> memberships = projectMemberRepository.findByUserIdAndDeletedAtIsNull(userId);

        return memberships.stream()
            .map(ProjectMember::getProject)
            .filter(project -> project.getDeletedAt() == null)
            .map(project -> {
                List<ProjectMember> members = projectMemberRepository.findByProjectAndDeletedAtIsNull(
                    project);
                List<MemberDto> memberDtos = members.stream()
                    .map(pm -> new MemberDto(
                        pm.getUser().getId(),
                        pm.getUser().getNickname(),
                        pm.getUser().getFileName()))
                    .collect(Collectors.toList());

                return new ProjectResponse(
                    project.getId(),
                    project.getTitle(),
                    project.getColor() != null ? project.getColor().name() : "E3EEFC", //version 1 default color
                    memberDtos
                );
            })
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectDetail(Long projectId, Long userId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!project.getMember().getId().equals(userId)) {
            throw new SecurityException("Only project creator can view this project");
        }

        List<ProjectMember> members = projectMemberRepository.findByProjectAndDeletedAtIsNull(project);
        List<MemberDto> memberDtos = members.stream()
            .map(pm -> new MemberDto(
                pm.getUser().getId(),
                pm.getUser().getNickname(),
                pm.getUser().getFileName()))
            .collect(Collectors.toList());

        return new ProjectResponse(
            project.getId(),
            project.getTitle(),
            project.getColor() != null ? project.getColor().name() : null,
            memberDtos
        );
    }

    @Transactional
    public void deleteProject(Long projectId, Long userId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!project.getMember().getId().equals(userId)) {
            throw new SecurityException("Only project creator can delete this project");
        }

        Project deltetedProject = project.toBuilder().deletedAt(LocalDateTime.now()).build();
        projectRepository.save(deltetedProject);

        List<ProjectMember> members = projectMemberRepository.findByProjectAndDeletedAtIsNull(project);
        for (ProjectMember member : members) {
            member.setDeletedAt(LocalDateTime.now());
        }
        projectMemberRepository.saveAll(members);

        // TODO: Send deletion notification
    }

    @Transactional
    public void updateProject(Long projectId, ProjectCreateRequest request, Long userId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!project.getMember().getId().equals(userId)) {
            throw new SecurityException("Only project creator can update this project");
        }

        if (request.getTitle() != null && request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("title must not be blank if provided");
        }

        List<User> updatedUsers = new ArrayList<>();
        if (request.getAddedMembers() != null) {
            for (ProjectCreateRequest.AddedMember member : request.getAddedMembers()) {
                User user = userRepository.findByNicknameAndDeletedAtIsNull(member.getNickname())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + member.getNickname()));
                updatedUsers.add(user);
            }
        }

        if (updatedUsers.size() + 1 > 8) {
            throw new IllegalArgumentException("Too many members");
        }

        // 프로젝트 업데이트
        project.toBuilder().title(request.getTitle()).updatedAt(LocalDateTime.now()).build();
        projectRepository.save(project);

        // 멤버 업데이트
        List<ProjectMember> existing = projectMemberRepository.findByProjectAndDeletedAtIsNull(project);
        List<ProjectMember> toRemove = existing.stream()
            .filter(pm -> !pm.getUser().getId().equals(userId) && updatedUsers.stream().noneMatch(u -> u.getId().equals(pm.getUser().getId())))
            .collect(Collectors.toList());

        toRemove.forEach(pm -> pm.setDeletedAt(LocalDateTime.now()));
        projectMemberRepository.saveAll(toRemove);

        for (int i = 0; i < updatedUsers.size(); i++) {
            User user = updatedUsers.get(i);
            ProjectCreateRequest.AddedMember member = request.getAddedMembers().get(i);

            Optional<ProjectMember> existingMember = existing.stream()
                .filter(pm -> pm.getUser().getId().equals(user.getId()))
                .findFirst();

            if (existingMember.isEmpty()) {
                projectMemberRepository.save(ProjectMember.builder()
                    .project(project)
                    .user(user)
                    .position(member.getPosition())
                    .role(Role.PARTICIPANT)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
            }
        }
    }

    @Transactional
    public void createProjectWiki(Long projectId, String content, User loginUser) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!project.getMember().getId().equals(loginUser.getId())) {
            throw new SecurityException("Only project creator can create wiki");
        }

        aiService.createProjectWiki(projectId, content);
    }

    @Transactional
    public void updateProjectWiki(Long projectId, String content, User loginUser) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!project.getMember().getId().equals(loginUser.getId())) {
            throw new SecurityException("Only project creator can update wiki");
        }

        aiService.updateProjectWiki(projectId, content);
    }

    @Transactional(readOnly = true)
    public String getProjectWiki(Long projectId, User loginUser) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!project.getMember().getId().equals(loginUser.getId())) {
            throw new SecurityException("Only project creator can view wiki");
        }

        return aiService.getProjectWiki(projectId);
    }
}
