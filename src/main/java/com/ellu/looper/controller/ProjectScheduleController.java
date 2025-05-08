package com.ellu.looper.controller;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.CurrentUser;
import com.ellu.looper.commons.PreviewHolder;
import com.ellu.looper.dto.schedule.ProjectScheduleCreateRequest;
import com.ellu.looper.dto.schedule.ProjectScheduleResponse;
import com.ellu.looper.dto.schedule.ProjectScheduleUpdateRequest;
import com.ellu.looper.entity.User;
import com.ellu.looper.service.ProjectScheduleService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@RequestMapping("/projects/{projectId}")
@RequiredArgsConstructor
public class ProjectScheduleController {

  private final ProjectScheduleService scheduleService;
  private final PreviewHolder previewHolder;

  @PostMapping("/schedules")
  public ResponseEntity<ApiResponse<List<ProjectScheduleResponse>>> createSchedules(
      @PathVariable Long projectId,
      @RequestBody ProjectScheduleCreateRequest request,
      @CurrentUser Long userId) {
    List<ProjectScheduleResponse> result =
        scheduleService.createSchedules(projectId, userId, request);
    return ResponseEntity.ok(new ApiResponse<>("project_daily_schedule", result));
  }

  @PatchMapping("/schedules/{scheduleId}")
  public ResponseEntity<ApiResponse<ProjectScheduleResponse>> updateSchedule(
      @PathVariable Long projectId,
      @PathVariable Long scheduleId,
      @RequestBody ProjectScheduleUpdateRequest request,
      @CurrentUser Long userId) {
    ProjectScheduleResponse result =
        scheduleService.updateSchedule(projectId, scheduleId, userId, request);
    return ResponseEntity.ok(new ApiResponse<>("schedule_updated", result));
  }

  @DeleteMapping("/schedules/{scheduleId}")
  public ResponseEntity<Void> deleteSchedule(
      @PathVariable Long projectId, @PathVariable Long scheduleId, @CurrentUser Long userId) {
    scheduleService.deleteSchedule(scheduleId, userId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/schedules/daily")
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

  @GetMapping("/schedules/weekly")
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

//  @GetMapping("/schedules/monthly")
//  public ResponseEntity<?> getMonthlySchedules(
//      @PathVariable Long projectId,
//      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
//    if (month == null) {
//      return ResponseEntity.badRequest()
//          .body(
//              new ApiResponse<>(
//                  "validation_failed",
//                  Map.of(
//                      "errors",
//                      Map.of(
//                          "month",
//                          "Missing or invalid month parameter. Format must be YYYY-MM."))));
//    }
//
//    Map<String, List<ProjectScheduleResponse>> schedules =
//        scheduleService.getMonthlySchedules(projectId, month);
//    return ResponseEntity.ok(new ApiResponse<>("project_monthly_schedule", schedules));
//  }

  @GetMapping("/schedules/monthly")
  public ResponseEntity<ApiResponse<?>> getMonthlySchedules(
      @PathVariable Long projectId,
      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {

    if (month == null) {
      return ResponseEntity.badRequest()
          .body(new ApiResponse<>(
              "validation_failed",
              Map.of("errors",
                  Map.of("month", "Missing or invalid month parameter. Format must be YYYY-MM."))));
    }

    YearMonth prevMonth = month.minusMonths(1);
    YearMonth nextMonth = month.plusMonths(1);

    LocalDateTime startDate = prevMonth.atDay(1).atStartOfDay();
    LocalDateTime endDate = nextMonth.atEndOfMonth().plusDays(1).atStartOfDay().minusNanos(1);

    // 전체 범위 일정 가져오기
    Map<String, List<ProjectScheduleResponse>> allSchedules =
        scheduleService.getSchedulesByRange(projectId, startDate, endDate);

    // 월별로 그룹화
    Map<String, Map<LocalDate, List<ProjectScheduleResponse>>> groupedByMonth = new HashMap<>();
    groupedByMonth.put(prevMonth.toString(), new HashMap<>());
    groupedByMonth.put(month.toString(), new HashMap<>());
    groupedByMonth.put(nextMonth.toString(), new HashMap<>());

    for (Entry<String, List<ProjectScheduleResponse>> entry : allSchedules.entrySet()) {
      LocalDate date = LocalDate.parse(entry.getKey());
      String key = YearMonth.from(date).toString();
      if (groupedByMonth.containsKey(key)) {
        groupedByMonth.get(key).put(date, entry.getValue());
      }
    }

    return ResponseEntity.ok(
        new ApiResponse<>("project_monthly_schedule", groupedByMonth));
  }

  @GetMapping("/schedules/yearly")
  public ResponseEntity<ApiResponse<Map<String, ?>>> getYearlySchedules(
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
    return ResponseEntity.ok(new ApiResponse<>("project_yearly_schedule", schedules));
  }

  @GetMapping("/tasks/preview")
  public DeferredResult<ResponseEntity<?>> getPreview(@PathVariable Long projectId,
      @CurrentUser Long UserId) {
    DeferredResult<ResponseEntity<?>> result = new DeferredResult<>(60000L); // 60초 타임아웃

    // 응답 대기 등록
    previewHolder.register(projectId, result);

    // 타임아웃 처리
    result.onTimeout(() -> {
      previewHolder.remove(projectId);
      result.setResult(ResponseEntity.status(HttpStatus.OK).body(
          ApiResponse.success("no_content_yet", null)
      ));
    });

    // 에러 처리 (네트워크 오류 등)
    result.onError(error -> {
      previewHolder.remove(projectId);
      result.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("internal_server_error")));
    });

    return result;
  }
}
