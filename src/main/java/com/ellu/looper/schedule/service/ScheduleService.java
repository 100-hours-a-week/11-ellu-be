package com.ellu.looper.schedule.service;

import com.ellu.looper.exception.ValidationException;
import com.ellu.looper.project.repository.ProjectRepository;
import com.ellu.looper.schedule.dto.ProjectScheduleResponse;
import com.ellu.looper.schedule.dto.ScheduleCreateRequest;
import com.ellu.looper.schedule.dto.ScheduleResponse;
import com.ellu.looper.schedule.dto.ScheduleUpdateRequest;
import com.ellu.looper.schedule.entity.Schedule;
import com.ellu.looper.schedule.repository.ScheduleRepository;
import com.ellu.looper.user.entity.User;
import com.ellu.looper.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

  private final ScheduleRepository scheduleRepository;
  private final UserRepository memberRepository;
  private final ProjectRepository projectRepository;
  private final ProjectScheduleService projectScheduleService;

  private void validateTimeOrder(LocalDateTime startTime, LocalDateTime endTime) {
    if (endTime.isEqual(startTime) || endTime.isBefore(startTime)) {
      throw new IllegalArgumentException("start_time_must_be_in_the_future");
    }
  }

  @Transactional
  public ScheduleResponse createSchedule(Long memberId, ScheduleCreateRequest request) {
    User user =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new AccessDeniedException("unauthorized"));

    Map<String, String> errors = new HashMap<>();

    if (request.title() == null || request.title().isBlank()) {
      errors.put("title", "Title is required");
    }

    if (!request.startTime().isBefore(request.endTime())) {
      errors.put("start_time", "Start time must be in the future");
    }

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }

    Schedule schedule =
        Schedule.builder()
            .user(user)
            .title(request.title())
            .description(request.description())
            .isAiRecommended(request.aiRecommended())
            .isCompleted(false)
            .startTime(request.startTime())
            .endTime(request.endTime())
            .build();
    Schedule saved = scheduleRepository.save(schedule);
    return toResponse(saved, false);
  }

  @Transactional
  public ScheduleResponse updateSchedule(Long memberId, Long id, ScheduleUpdateRequest request) {
    Schedule existing =
        scheduleRepository
            .findByIdAndUserIdAndDeletedAtIsNull(id, memberId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or already deleted dto"));

    LocalDateTime newStart =
        request.startTime() != null ? request.startTime() : existing.getStartTime();
    LocalDateTime newEnd = request.endTime() != null ? request.endTime() : existing.getEndTime();

    validateTimeOrder(newStart, newEnd);

    Schedule updated =
        existing.toBuilder()
            .title(request.title() != null ? request.title() : existing.getTitle())
            .description(
                request.description() != null ? request.description() : existing.getDescription())
            .isCompleted(request.completed() != null ? request.completed() : existing.isCompleted())
            .startTime(request.startTime() != null ? request.startTime() : existing.getStartTime())
            .endTime(request.endTime() != null ? request.endTime() : existing.getEndTime())
            .build();

    return toResponse(scheduleRepository.save(updated), false);
  }

  @Transactional
  public void deleteSchedule(Long memberId, Long id) {
    Schedule schedule =
        scheduleRepository
            .findByIdAndUserIdAndDeletedAtIsNull(id, memberId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or already deleted dto"));
    schedule.toBuilder().deletedAt(LocalDateTime.now()).build();
    scheduleRepository.delete(schedule);
  }

  public List<ScheduleResponse> getDailySchedules(Long memberId, LocalDate date) {
    LocalDateTime start = date.atStartOfDay();
    LocalDateTime end = date.plusDays(1).atStartOfDay();

    List<Schedule> personalSchedules = scheduleRepository.findDailySchedules(memberId, start, end);

    List<ScheduleResponse> responses =
        personalSchedules.stream().map(s -> toResponse(s, false)).collect(Collectors.toList());

    List<ScheduleResponse> projectSchedules = getProjectDailySchedules(memberId, start, end);

    responses.addAll(projectSchedules);
    return responses;
  }

  public Map<LocalDate, List<ScheduleResponse>> getSchedulesByRange(
      Long memberId, LocalDate startDate, LocalDate endDate) {
    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusNanos(1); // 범위 끝을 포함시키기 위해

    List<Schedule> personal = scheduleRepository.findSchedulesBetween(memberId, start, end);
    List<ScheduleResponse> responses = personal.stream().map(s -> toResponse(s, false)).toList();

    List<ScheduleResponse> project =
        getProjectSchedules(memberId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
    List<ScheduleResponse> all = new ArrayList<>();
    all.addAll(responses);
    all.addAll(project);
    return all.stream().collect(Collectors.groupingBy(r -> r.startTime().toLocalDate()));
  }

  @Transactional(readOnly = true)
  public List<ScheduleResponse> getProjectSchedules(
      Long memberId, LocalDateTime start, LocalDateTime end) {

    List<ProjectScheduleResponse> projectSchedules =
        projectRepository.findAllByMemberId(memberId).stream()
            .flatMap(
                project ->
                    projectScheduleService
                        .getSchedulesByRange(project.getId(), start, end)
                        .values()
                        .stream()
                        .flatMap(List::stream))
            .toList();
    return projectSchedules.stream()
        .map(
            ps ->
                new ScheduleResponse(
                    ps.id(),
                    ps.title(),
                    ps.description(),
                    ps.is_completed(),
                    false,
                    ps.is_project_schedule(),
                    ps.start_time(),
                    ps.end_time(),
                    ps.color()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ScheduleResponse> getProjectDailySchedules(
      Long memberId, LocalDateTime start, LocalDateTime end) {
    if (!start.toLocalDate().equals(end.minusNanos(1).toLocalDate())) {
      throw new IllegalArgumentException("일일 조회가 아닙니다. 일일 범위만 getDailySchedules에서 지원됩니다.");
    }

    List<ProjectScheduleResponse> projectSchedules =
        projectRepository.findAllByMemberId(memberId).stream()
            .flatMap(
                project ->
                    projectScheduleService
                        .getDailySchedules(project.getId(), start.toLocalDate())
                        .stream())
            .toList();

    return projectSchedules.stream()
        .map(
            ps ->
                new ScheduleResponse(
                    ps.id(),
                    ps.title(),
                    ps.description(),
                    ps.is_completed(),
                    false,
                    ps.is_project_schedule(),
                    ps.start_time(),
                    ps.end_time(),
                    ps.color()))
        .toList();
  }

  public ScheduleResponse toResponse(Schedule s, boolean isProject) {
    return ScheduleResponse.builder()
        .id(s.getId())
        .title(s.getTitle())
        .description(s.getDescription())
        .completed(s.isCompleted())
        .aiRecommended(s.isAiRecommended())
        .projectSchedule(isProject)
        .startTime(s.getStartTime())
        .endTime(s.getEndTime())
        .color(null)
        .build();
  }
}
