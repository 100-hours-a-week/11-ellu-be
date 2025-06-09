package com.ellu.looper.schedule.service;

import com.ellu.looper.commons.enums.NotificationType;
import com.ellu.looper.exception.ValidationException;
import com.ellu.looper.kafka.NotificationProducer;
import com.ellu.looper.kafka.dto.NotificationMessage;
import com.ellu.looper.notification.entity.Notification;
import com.ellu.looper.notification.entity.NotificationTemplate;
import com.ellu.looper.notification.repository.NotificationRepository;
import com.ellu.looper.notification.repository.NotificationTemplateRepository;
import com.ellu.looper.notification.service.NotificationService;
import com.ellu.looper.project.entity.Project;
import com.ellu.looper.project.entity.ProjectMember;
import com.ellu.looper.project.repository.ProjectMemberRepository;
import com.ellu.looper.project.repository.ProjectRepository;
import com.ellu.looper.schedule.dto.AssigneeDto;
import com.ellu.looper.schedule.dto.ProjectScheduleCreateRequest;
import com.ellu.looper.schedule.dto.ProjectScheduleResponse;
import com.ellu.looper.schedule.dto.StompProjectScheduleUpdateRequest;
import com.ellu.looper.schedule.entity.Assignee;
import com.ellu.looper.schedule.entity.ProjectSchedule;
import com.ellu.looper.schedule.entity.Schedule;
import com.ellu.looper.schedule.repository.AssigneeRepository;
import com.ellu.looper.schedule.repository.ProjectScheduleRepository;
import com.ellu.looper.schedule.repository.ScheduleRepository;
import com.ellu.looper.user.entity.User;
import com.ellu.looper.user.repository.UserRepository;
import com.ellu.looper.user.service.ProfileImageService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectScheduleService {

  private final ProjectScheduleRepository projectScheduleRepository;
  private final ScheduleRepository scheduleRepository;
  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final AssigneeRepository assigneeRepository;
  private final NotificationRepository notificationRepository;
  private final NotificationTemplateRepository notificationTemplateRepository;
  private final ProfileImageService profileImageService;
  private final NotificationService notificationService;
  private final NotificationProducer notificationProducer;

  private void validateTimeOrder(LocalDateTime startTime, LocalDateTime endTime) {
    if (startTime != null && endTime != null) {
      if (endTime.isEqual(startTime) || endTime.isBefore(startTime)) {
        throw new IllegalArgumentException("종료 시각은 시작 시각보다 나중이어야 합니다.");
      }
    }
  }

  @Transactional
  public List<ProjectScheduleResponse> createSchedules(
      Long projectId, Long userId, ProjectScheduleCreateRequest request) {
    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid project ID"));

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid user ID"));

    Map<String, String> errors = new HashMap<>();

    int index = 0;
    for (ProjectScheduleCreateRequest.ProjectScheduleDto dto : request.getProject_schedules()) {
      String prefix = "project_schedules[" + index + "]";

      if (dto.getTitle() == null || dto.getTitle().isBlank()) {
        errors.put(prefix + ".title", "Title is required");
      }

      if (dto.getStartTime() == null) {
        errors.put(prefix + ".start_time", "Start time is required");
      }

      if (dto.getEndTime() == null) {
        errors.put(prefix + ".end_time", "End time is required");
      }

      if (dto.getStartTime() != null
          && dto.getEndTime() != null
          && !dto.getStartTime().isBefore(dto.getEndTime())) {
        errors.put(prefix + ".time", "End time must be after start time");
      }
      index++;
    }

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }

    // assign project schedules according to members' positions
    List<ProjectScheduleResponse> responses = new ArrayList<>();

    for (ProjectScheduleCreateRequest.ProjectScheduleDto dto : request.getProject_schedules()) {
      // match by project id and position
      List<ProjectMember> matchedMembers =
          projectMemberRepository.findByProjectIdAndPositionAndDeletedAtIsNull(
              projectId, dto.getPosition());

      ProjectSchedule schedule =
          ProjectSchedule.builder()
              .project(project)
              .user(user)
              .title(dto.getTitle())
              .description(dto.getDescription())
              .startTime(dto.getStartTime())
              .endTime(dto.getEndTime())
              .isCompleted(dto.getCompleted())
              .position(dto.getPosition())
              .build();

      for (ProjectMember member : matchedMembers) {
        Assignee assignee = Assignee.builder().user(member.getUser()).build();
        schedule.addAssignee(assignee);
      }

      schedule = projectScheduleRepository.save(schedule);

      responses.add(toResponse(schedule));

      // Send schedule creation notification
      sendScheduleNotification(
          NotificationType.SCHEDULE_CREATED,
          matchedMembers.stream().map(ProjectMember::getUser).toList(),
          userId,
          project,
          schedule);
    }
    return responses;
  }

  @Transactional
  public ProjectScheduleResponse updateSchedule(
      Long scheduleId, Long userId, StompProjectScheduleUpdateRequest request) {
    ProjectSchedule schedule =
        projectScheduleRepository
            .findByIdAndDeletedAtIsNull(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));

    validateTimeOrder(request.start_time(), request.end_time());

    schedule.update(
        request.title(),
        request.description(),
        request.start_time(),
        request.end_time(),
        request.position(),
        request.completed());
    log.info("업데이트 완료됨");
    Set<User> notificationTargets = new HashSet<>();

    // If assignee is newly added or removed, update assignee table
    if (request.position() != null) {
      List<Assignee> currentAssignees =
          assigneeRepository.findByProjectScheduleIdAndDeletedAtIsNull(scheduleId);

      // Soft delete existing assignees
      for (Assignee assignee : currentAssignees) {
        assignee.softDelete();
        notificationTargets.add(assignee.getUser());
      }
      // Add new assignees
      List<ProjectMember> matchingMembers =
          projectMemberRepository.findByProjectIdAndPosition(
              schedule.getProject().getId(), request.position());

      for (ProjectMember member : matchingMembers) {
        Assignee newAssignee =
            Assignee.builder().projectSchedule(schedule).user(member.getUser()).build();

        schedule.addAssignee(newAssignee);
        assigneeRepository.save(newAssignee);

        notificationTargets.add(member.getUser());
      }
    }

    List<Assignee> assignees =
        assigneeRepository.findByProjectScheduleIdAndDeletedAtIsNull(scheduleId);

    // Send schedule update notification
    sendScheduleNotification(
        NotificationType.SCHEDULE_UPDATED,
        new ArrayList<>(notificationTargets),
        userId,
        schedule.getProject(),
        schedule);
    return new ProjectScheduleResponse(
        schedule.getId(),
        schedule.getTitle(),
        schedule.getDescription(),
        schedule.getStartTime(),
        schedule.getEndTime(),
        schedule.isCompleted(),
        true,
        schedule.getProject().getColor(),
        schedule.getPosition(),
        convertToAssigneeDtos(assignees));
  }

  @Transactional
  public void deleteSchedule(Long scheduleId, Long userId) {
    ProjectSchedule schedule =
        projectScheduleRepository
            .findByIdAndDeletedAtIsNull(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));

    //  delete schedule assignee
    List<Assignee> assignees =
        assigneeRepository.findByProjectScheduleIdAndDeletedAtIsNull(scheduleId);
    for (Assignee assignee : assignees) {
      assignee.softDelete();
    }
    // delete project schedule
    schedule.softDelete();

    // Send schedule deletion notification
    List<User> receivers = assignees.stream().map(Assignee::getUser).collect(Collectors.toList());
    sendScheduleNotification(
        NotificationType.SCHEDULE_DELETED, receivers, userId, schedule.getProject(), schedule);
  }

  @Transactional(readOnly = true)
  public List<ProjectScheduleResponse> getDailySchedules(Long projectId, LocalDate day) {
    LocalDateTime start = day.atStartOfDay();
    LocalDateTime end = day.plusDays(1).atStartOfDay().minusNanos(1);
    List<ProjectSchedule> dailyProjectSchedules =
        projectScheduleRepository.findDailyProjectSchedules(projectId, start, end);
    List<ProjectScheduleResponse> responses =
        dailyProjectSchedules.stream().map(s -> toResponse(s, true)).collect(Collectors.toList());
    return responses;
  }

  private ProjectScheduleResponse toResponse(ProjectSchedule s, boolean isProject) {
    return ProjectScheduleResponse.builder()
        .id(s.getId())
        .title(s.getTitle())
        .description(s.getDescription())
        .start_time(s.getStartTime())
        .end_time(s.getEndTime())
        .is_project_schedule(isProject)
        .color(s.getProject().getColor())
        .assignees(convertToAssigneeDtos(s.getAssignees()))
        .build();
  }

  @Transactional(readOnly = true)
  public Map<String, List<ProjectScheduleResponse>> getWeeklySchedules(
      Long projectId, LocalDate startDate) {
    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = startDate.plusDays(7).atStartOfDay().minusNanos(1);
    return getSchedulesByRange(projectId, start, end);
  }

  @Transactional(readOnly = true)
  public Map<String, List<ProjectScheduleResponse>> getMonthlySchedules(
      Long projectId, YearMonth month) {
    LocalDateTime start = month.atDay(1).atStartOfDay();
    LocalDateTime end = month.atEndOfMonth().atTime(LocalTime.MAX);
    return getSchedulesByRange(projectId, start, end);
  }

  @Transactional(readOnly = true)
  public Map<String, List<ProjectScheduleResponse>> getYearlySchedules(Long projectId, Year year) {
    LocalDateTime start = year.atDay(1).atStartOfDay();
    LocalDateTime end = year.atMonth(12).atEndOfMonth().atTime(LocalTime.MAX);
    return getSchedulesByRange(projectId, start, end);
  }

  @Transactional(readOnly = true)
  public Map<String, List<ProjectScheduleResponse>> getSchedulesByRange(
      Long projectId, LocalDateTime start, LocalDateTime end) {
    Project project =
        projectRepository
            .findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid project ID"));

    List<ProjectSchedule> schedules =
        projectScheduleRepository.findSchedulesBetween(projectId, start, end);

    LinkedHashMap<String, List<ProjectScheduleResponse>> collect =
        schedules.stream()
            .collect(
                Collectors.groupingBy(
                    s -> s.getStartTime().toLocalDate().toString(),
                    LinkedHashMap::new,
                    Collectors.mapping(
                        s ->
                            new ProjectScheduleResponse(
                                s.getId(),
                                s.getTitle(),
                                s.getDescription(),
                                s.getStartTime(),
                                s.getEndTime(),
                                s.isCompleted(),
                                true,
                                s.getProject().getColor(),
                                s.getPosition(),
                                convertToAssigneeDtos(s.getAssignees())),
                        Collectors.toList())));
    return collect;
  }

  public List<AssigneeDto> convertToAssigneeDtos(List<Assignee> assignees) {
    return assignees.stream()
        .map(
            a ->
                new AssigneeDto(
                    a.getUser().getNickname(),
                    profileImageService.getProfileImageUrl(a.getUser().getFileName())))
        .toList();
  }

  private void sendScheduleNotification(
      NotificationType type,
      List<User> receivers,
      Long userId,
      Project project,
      ProjectSchedule schedule) {
    User creator =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    // Notification 생성
    NotificationTemplate inviteTemplate =
        notificationTemplateRepository
            .findByType(type)
            .orElseThrow(() -> new IllegalArgumentException("해당 스케줄 알림 타입에 대한 템플릿이 존재하지 않습니다."));

    Map<String, Object> payload = new HashMap<>();
    payload.put("project", project.getTitle());
    payload.put("schedule", schedule.getTitle());

    for (User receiver : receivers) {

      Notification notification =
          Notification.builder()
              .sender(creator)
              .receiver(receiver)
              .project(project)
              .template(inviteTemplate)
              .payload(payload)
              .createdAt(LocalDateTime.now())
              .build();
      notificationRepository.save(notification);

      // Kafka를 통해 알림 메시지 전송
      NotificationMessage message =
          new NotificationMessage(
              type.toString(),
              notification.getId(),
              project.getId(),
              creator.getId(),
              List.of(receiver.getId()),
              notificationService.renderScheduleTemplate(
                  inviteTemplate.getTemplate(), notification));

      log.info("TRYING TO SEND KAFKA MESSAGE: {}", message.getMessage());
      notificationProducer.sendNotification(message);
    }
  }

  @Transactional
  public void takeSchedule(Long projectId, Long scheduleId, Long userId) {
    ProjectSchedule schedule =
        projectScheduleRepository
            .findByIdAndDeletedAtIsNull(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));

    // Check if the user has already taken this schedule
    boolean alreadyTaken =
        assigneeRepository.existsByUserIdAndProjectScheduleIdAndDeletedAtIsNull(userId, scheduleId);
    if (alreadyTaken) {
      throw new IllegalArgumentException("Schedule already taken");
    }

    // Check if this user has the appropriate position
    ProjectMember projectMember =
        projectMemberRepository
            .findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Project Member not found"));

    // TODO: 본인 포지션 일정만 가져갈 있도록 권한 처리 완료 -> 웹소켓 에러 처리 후 권한 처리할 예정임
//    if (schedule.getPosition() != projectMember.getPosition()) {
//      throw new AccessDeniedException(
//          String.format(
//              "Access denied: user %d is not authorized to take schedule %d due to position mismatch.",
//              userId, scheduleId));
//    }

    // add this user to assignee table
    User user = projectMember.getUser();

    assigneeRepository.save(new Assignee(schedule, user));

    // Add a new personal schedule
    Schedule personalSchedule =
        Schedule.builder()
            .user(user)
            .title(schedule.getTitle())
            .description(schedule.getDescription())
            .startTime(schedule.getStartTime())
            .endTime(schedule.getEndTime())
            .build();
    scheduleRepository.save(personalSchedule);
  }

  public ProjectScheduleResponse toResponse(ProjectSchedule schedule) {
    return new ProjectScheduleResponse(
        schedule.getId(),
        schedule.getTitle(),
        schedule.getDescription(),
        schedule.getStartTime(),
        schedule.getEndTime(),
        schedule.isCompleted(),
        true,
        schedule.getProject().getColor(),
        schedule.getPosition(),
        schedule.getAssignees().stream()
            .map(
                assignee ->
                    new AssigneeDto(
                        assignee.getUser().getNickname(),
                        profileImageService.getProfileImageUrl(assignee.getUser().getFileName())))
            .toList());
  }
}
