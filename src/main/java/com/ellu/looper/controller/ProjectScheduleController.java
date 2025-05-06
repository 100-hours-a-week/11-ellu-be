package com.ellu.looper.controller;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.CurrentUser;
import com.ellu.looper.dto.schedule.ProjectScheduleCreateRequest;
import com.ellu.looper.dto.schedule.ProjectScheduleResponse;
import com.ellu.looper.dto.schedule.ProjectScheduleUpdateRequest;
import com.ellu.looper.service.ProjectScheduleService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects/{projectId}/schedules")
@RequiredArgsConstructor
public class ProjectScheduleController {

  private final ProjectScheduleService scheduleService;

  @PostMapping
  public ResponseEntity<ApiResponse<List<ProjectScheduleResponse>>> createSchedules(
      @PathVariable Long projectId,
      @RequestBody ProjectScheduleCreateRequest request,
      @CurrentUser Long userId
  ) {
    List<ProjectScheduleResponse> result = scheduleService.createSchedules(projectId, userId,
        request);
    return ResponseEntity.ok(
        new ApiResponse<>("project_daily_schedule", result));
  }


  @PatchMapping("/{scheduleId}")
  public ResponseEntity<ApiResponse<ProjectScheduleResponse>> updateSchedule(
      @PathVariable Long projectId,
      @PathVariable Long scheduleId,
      @RequestBody ProjectScheduleUpdateRequest request,
      @CurrentUser Long userId
  ) {
    ProjectScheduleResponse result = scheduleService.updateSchedule(projectId, scheduleId, userId,
        request);
    return ResponseEntity.ok(new ApiResponse<>("schedule_updated", result));
  }

  @DeleteMapping("/{scheduleId}")
  public ResponseEntity<Void> deleteSchedule(
      @PathVariable Long projectId,
      @PathVariable Long scheduleId,
      @CurrentUser Long userId
  ) {
    scheduleService.deleteSchedule(scheduleId, userId);
    return ResponseEntity.noContent().build();
  }


  @GetMapping("/daily")
  public ResponseEntity<ApiResponse<?>> getDailySchedules(
      @PathVariable Long projectId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day) {
    if (day == null) {
      return ResponseEntity.badRequest().body(
          new ApiResponse<>("validation_failed", Map.of("errors", Map.of("day", "Missing or invalid date parameter. Format must be YYYY-MM-DD.")))
      );
    }

    List<ProjectScheduleResponse> schedules = scheduleService.getDailySchedules(projectId, day);
    return ResponseEntity.ok(new ApiResponse<>("project_daily_schedule", schedules));
  }

  @GetMapping("/weekly")
  public ResponseEntity<ApiResponse<Map<String, ?>>> getWeeklySchedules(
      @PathVariable Long projectId,
      @RequestParam(required = false, name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
    if (startDate == null) {
      return ResponseEntity.badRequest().body(
          new ApiResponse<>("validation_failed", Map.of("errors", Map.of("startDate", "Missing or invalid date parameter. Format must be YYYY-MM-DD.")))
      );
    }

    Map<String, List<ProjectScheduleResponse>> schedules = scheduleService.getWeeklySchedules(projectId, startDate);
    return ResponseEntity.ok(new ApiResponse<>("project_weekly_schedule", schedules));
  }

  @GetMapping("/monthly")
  public ResponseEntity<ApiResponse<Map<String,?>>> getMonthlySchedules(
      @PathVariable Long projectId,
      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
    if (month == null) {
      return ResponseEntity.badRequest().body(
          new ApiResponse<>("validation_failed", Map.of("errors", Map.of("month", "Missing or invalid month parameter. Format must be YYYY-MM.")))
      );
    }

    Map<String, List<ProjectScheduleResponse>> schedules = scheduleService.getMonthlySchedules(projectId, month);
    return ResponseEntity.ok(new ApiResponse<>("project_monthly_schedule", schedules));
  }

  @GetMapping("/yearly")
  public ResponseEntity<ApiResponse<Map<String, ?>>> getYearlySchedules(
      @PathVariable Long projectId,
      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy") Year year) {
    if (year == null) {
      return ResponseEntity.badRequest().body(
          new ApiResponse<>("validation_failed", Map.of("errors", Map.of("year", "Missing or invalid year parameter. Format must be YYYY.")))
      );
    }

    Map<String, List<ProjectScheduleResponse>> schedules = scheduleService.getYearlySchedules(projectId, year);
    return ResponseEntity.ok(new ApiResponse<>("project_yearly_schedule", schedules));
  }
}

