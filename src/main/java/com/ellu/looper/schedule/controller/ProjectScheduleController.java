package com.ellu.looper.schedule.controller;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.CurrentUser;
import com.ellu.looper.project.repository.ProjectMemberRepository;
import com.ellu.looper.schedule.dto.ProjectScheduleCreateRequest;
import com.ellu.looper.schedule.dto.ProjectScheduleResponse;
import com.ellu.looper.schedule.dto.StompProjectScheduleUpdateRequest;
import com.ellu.looper.schedule.service.PreviewHolder;
import com.ellu.looper.schedule.service.ProjectScheduleService;
import com.ellu.looper.schedule.service.ScheduleHolder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectScheduleController {

  private final ProjectScheduleService scheduleService;
  private final PreviewHolder previewHolder;
  private final ScheduleHolder scheduleHolder;
  private final ProjectMemberRepository projectMemberRepository;

  @PostMapping("/{projectId}/schedules")
  public DeferredResult<ResponseEntity<?>> createSchedules(
      @PathVariable Long projectId,
      @RequestBody ProjectScheduleCreateRequest request,
      @CurrentUser Long userId) { // 프로젝트 멤버인지 확인
    projectMemberRepository
        .findByProjectIdAndUserIdAndDeletedAtIsNull(projectId, userId)
        .orElseThrow(() -> new AccessDeniedException("Not a member of this project"));
    DeferredResult<ResponseEntity<?>> result = new DeferredResult<>(300000L); // 5분 타임아웃
    // 요청 등록
    scheduleHolder.register(projectId, userId, request, result);
    result.onTimeout(
        () -> {
          scheduleHolder.remove(projectId, userId);
          result.setResult(
              ResponseEntity.status(HttpStatus.ACCEPTED)
                  .body(
                      ApiResponse.success(
                          "still_processing",
                          Map.of(
                              "message",
                              "Schedule creation is still processing.",
                              "data",
                              List.of()))));
        });
    result.onError(
        (error) -> {
          scheduleHolder.remove(projectId, userId);
          result.setResult(
              ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("internal_server_error")));
        });
    return result;
  }

  @PatchMapping("/schedules/{scheduleId}")
  public ResponseEntity<ApiResponse<ProjectScheduleResponse>> updateSchedule(
      @PathVariable Long scheduleId,
      @RequestBody StompProjectScheduleUpdateRequest request,
      @CurrentUser Long userId) {
    ProjectScheduleResponse result = scheduleService.updateSchedule(scheduleId, userId, request);
    return ResponseEntity.ok(new ApiResponse<>("schedule_updated", result));
  }

  @DeleteMapping("/schedules/{scheduleId}")
  public ResponseEntity<Void> deleteSchedule(
      @PathVariable Long scheduleId, @CurrentUser Long userId) {
    scheduleService.deleteSchedule(scheduleId, userId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{projectId}/schedules/daily")
  public ResponseEntity<ApiResponse<?>> getDailySchedules(
      @PathVariable Long projectId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate day) {
    if (day == null) {
      return ResponseEntity.badRequest()
          .body(
              new ApiResponse<>(
                  "validation_failed",
                  Map.of(
                      "errors",
                      Map.of(
                          "day",
                          "Missing or invalid date parameter. Format must be YYYY-MM-DD."))));
    }

    List<ProjectScheduleResponse> schedules = scheduleService.getDailySchedules(projectId, day);
    return ResponseEntity.ok(new ApiResponse<>("project_daily_schedule", schedules));
  }

  @GetMapping("/{projectId}/schedules/weekly")
  public ResponseEntity<ApiResponse<Map<String, ?>>> getWeeklySchedules(
      @PathVariable Long projectId,
      @RequestParam(required = false, name = "startDate")
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate) {
    if (startDate == null) {
      return ResponseEntity.badRequest()
          .body(
              new ApiResponse<>(
                  "validation_failed",
                  Map.of(
                      "errors",
                      Map.of(
                          "startDate",
                          "Missing or invalid date parameter. Format must be YYYY-MM-DD."))));
    }

    Map<String, List<ProjectScheduleResponse>> schedules =
        scheduleService.getWeeklySchedules(projectId, startDate);
    return ResponseEntity.ok(new ApiResponse<>("project_weekly_schedule", schedules));
  }

  @GetMapping("/{projectId}/schedules/monthly")
  public ResponseEntity<ApiResponse<?>> getMonthlySchedules(
      @PathVariable Long projectId,
      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {

    if (month == null) {
      return ResponseEntity.badRequest()
          .body(
              new ApiResponse<>(
                  "validation_failed",
                  Map.of(
                      "errors",
                      Map.of(
                          "month",
                          "Missing or invalid month parameter. Format must be YYYY-MM."))));
    }

    YearMonth prevMonth = month.minusMonths(1);
    YearMonth nextMonth = month.plusMonths(1);

    LocalDateTime startDate = prevMonth.atDay(1).atStartOfDay();
    LocalDateTime endDate = nextMonth.atEndOfMonth().plusDays(1).atStartOfDay().minusNanos(1);

    // 전체 범위 일정 가져오기
    Map<String, List<ProjectScheduleResponse>> allSchedules =
        scheduleService.getSchedulesByRange(projectId, startDate, endDate);

    List<ProjectScheduleResponse> flattenedSchedules =
        allSchedules.values().stream().flatMap(List::stream).collect(Collectors.toList());

    return ResponseEntity.ok(new ApiResponse<>("project_monthly_schedule", flattenedSchedules));
  }

  @GetMapping("/{projectId}/schedules/yearly")
  public ResponseEntity<ApiResponse<?>> getYearlySchedules(
      @PathVariable Long projectId,
      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy") Year year) {
    if (year == null) {
      return ResponseEntity.badRequest()
          .body(
              new ApiResponse<>(
                  "validation_failed",
                  Map.of(
                      "errors",
                      Map.of("year", "Missing or invalid year parameter. Format must be YYYY."))));
    }

    Map<String, List<ProjectScheduleResponse>> schedules =
        scheduleService.getYearlySchedules(projectId, year);
    // 모든 일정 변환
    List<ProjectScheduleResponse> flattenedSchedules =
        schedules.values().stream().flatMap(List::stream).collect(Collectors.toList());

    return ResponseEntity.ok(new ApiResponse<>("project_yearly_schedule", flattenedSchedules));
  }

  @GetMapping("/{projectId}/tasks/preview")
  public DeferredResult<ResponseEntity<?>> getPreview(
      @PathVariable Long projectId, @CurrentUser Long userId) {
    // 프로젝트 멤버십 확인
    projectMemberRepository
        .findByProjectIdAndUserIdAndDeletedAtIsNull(projectId, userId)
        .orElseThrow(() -> new AccessDeniedException("Not a member of this project"));

    DeferredResult<ResponseEntity<?>> result = new DeferredResult<>(300000L); // 300초 타임아웃

    // 응답 대기 등록
    previewHolder.register(projectId, result);

    // 타임아웃 처리
    result.onTimeout(
        () -> {
          previewHolder.remove(projectId);
          result.setResult(
              ResponseEntity.ok()
                  .body(
                      ApiResponse.success(
                          "no_content_yet", Map.of("message", "processing", "data", List.of()))));
        });

    // 에러 처리 (네트워크 오류 등)
    result.onError(
        error -> {
          previewHolder.remove(projectId);
          result.setResult(
              ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("internal_server_error")));
        });

    return result;
  }

  @PatchMapping("/project/{projectId}/schedules/{projectScheduleId}/assignees")
  public ResponseEntity<ApiResponse<Void>> takeSchedule(
      @PathVariable Long projectId,
      @PathVariable Long projectScheduleId,
      @CurrentUser Long userId) {
    scheduleService.takeSchedule(projectId, projectScheduleId, userId);
    return ResponseEntity.ok(new ApiResponse<>("added_to_personal_schedule", null));
  }
}
