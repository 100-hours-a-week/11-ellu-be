package com.ellu.looper.service;

import com.ellu.looper.dto.schedule.ProjectScheduleCreateRequest;
import com.ellu.looper.dto.schedule.ProjectScheduleResponse;
import com.ellu.looper.dto.schedule.ProjectScheduleUpdateRequest;
import com.ellu.looper.entity.Project;
import com.ellu.looper.entity.ProjectSchedule;
import com.ellu.looper.entity.User;
import com.ellu.looper.exception.ValidationException;
import com.ellu.looper.repository.ProjectRepository;
import com.ellu.looper.repository.ProjectScheduleRepository;
import com.ellu.looper.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectScheduleService {

  private final ProjectScheduleRepository scheduleRepository;
  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;

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

    List<ProjectSchedule> saved =
        request.getProject_schedules().stream()
            //        .peek(dto -> validateTimeOrder(dto.getStartTime(), dto.getEndTime()))
            .map(
                dto ->
                    ProjectSchedule.builder()
                        .project(project)
                        .user(user)
                        .title(dto.getTitle())
                        .description(dto.getDescription())
                        .startTime(dto.getStartTime())
                        .endTime(dto.getEndTime())
                        .isCompleted(dto.getCompleted())
                        .build())
            .map(scheduleRepository::save)
            .toList();

    return saved.stream()
        .map(
            s ->
                new ProjectScheduleResponse(
                    s.getId(),
                    s.getTitle(),
                    s.getDescription(),
                    s.getStartTime(),
                    s.getEndTime(),
                    s.isCompleted(),
                    true))
        .toList();
  }

  @Transactional
  public ProjectScheduleResponse updateSchedule(
      Long projectId, Long scheduleId, Long userId, ProjectScheduleUpdateRequest request) {
    ProjectSchedule schedule =
        scheduleRepository
            .findByIdAndDeletedAtIsNull(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));

    if (!schedule.getUser().getId().equals(userId)) {
      throw new AccessDeniedException("Unauthorized");
    }

    validateTimeOrder(request.start_time(), request.end_time());

    schedule.update(
        request.title(), null, request.start_time(), request.end_time(), request.completed());
    return new ProjectScheduleResponse(
        schedule.getId(),
        schedule.getTitle(),
        schedule.getDescription(),
        schedule.getStartTime(),
        schedule.getEndTime(),
        schedule.isCompleted(),
        true);
  }

  @Transactional
  public void deleteSchedule(Long scheduleId, Long userId) {
    ProjectSchedule schedule =
        scheduleRepository
            .findByIdAndDeletedAtIsNull(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));

    if (!schedule.getUser().getId().equals(userId)) {
      throw new AccessDeniedException("Unauthorized");
    }

    schedule.softDelete();
  }

  @Transactional(readOnly = true)
  public List<ProjectScheduleResponse> getDailySchedules(Long projectId, LocalDate day) {
    LocalDateTime start = day.atStartOfDay();
    LocalDateTime end = day.plusDays(1).atStartOfDay().minusNanos(1);
    List<ProjectSchedule> dailyProjectSchedules =
        scheduleRepository.findDailyProjectSchedules(projectId, start, end);
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
        scheduleRepository.findSchedulesBetween(projectId, start, end);

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
                                true),
                        Collectors.toList())));
    return collect;
  }
}
