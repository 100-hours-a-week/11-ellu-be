package com.ellu.looper.schedule.controller;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.CurrentUser;
import com.ellu.looper.schedule.dto.PlanCreateRequest;
import com.ellu.looper.schedule.dto.ScheduleCreateRequest;
import com.ellu.looper.schedule.dto.ScheduleResponse;
import com.ellu.looper.schedule.dto.ScheduleUpdateRequest;
import com.ellu.looper.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@RestController
@RequestMapping("/user/schedules")
@RequiredArgsConstructor
@Slf4j
public class ScheduleController {

  private final ScheduleService scheduleService;

  @PostMapping
  public ResponseEntity<?> create(
      @CurrentUser Long userId, @RequestBody ScheduleCreateRequest request) {
    ScheduleResponse response = scheduleService.createSchedule(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of("message", "schedule_created", "data", response));
  }

  @PostMapping("/plan")
  public ResponseEntity<?> createPlan(
      @CurrentUser Long userId, @RequestBody PlanCreateRequest request) {
    List<ScheduleResponse> response = scheduleService.createPlan(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of("message", "plan_created", "data", response));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<?> update(
      @CurrentUser Long userId,
      @PathVariable Long id,
      @Valid @RequestBody ScheduleUpdateRequest request) {
    ScheduleResponse response = scheduleService.updateSchedule(userId, id, request);
    return ResponseEntity.ok(Map.of("message", "schedule_updated", "data", response));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@CurrentUser Long userId, @PathVariable Long id) {
    scheduleService.deleteSchedule(userId, id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/daily")
  public ResponseEntity<?> daily(@CurrentUser Long userId, @RequestParam String date) {
    LocalDate localDate;
    try {
      localDate = LocalDate.parse(date);
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body(
              Map.of(
                  "message",
                  "validation_failed",
                  "data",
                  Map.of(
                      "errors",
                      Map.of(
                          "date",
                          "Missing or invalid date parameter. Format must be YYYY-MM-DD."))));
    }
    List<ScheduleResponse> data = scheduleService.getDailySchedules(userId, localDate);
    return ResponseEntity.ok(Map.of("message", "daily_schedule", "data", data));
  }

  @GetMapping("/weekly")
  public ResponseEntity<?> weekly(@CurrentUser Long userId, @RequestParam String startDate) {
    try {
      LocalDate start = LocalDate.parse(startDate);
      Map<LocalDate, List<ScheduleResponse>> data =
          scheduleService.getSchedulesByRange(userId, start, start.plusDays(6));
      return ResponseEntity.ok(Map.of("message", "weekly_schedule", "data", data));
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body(
              Map.of(
                  "message",
                  "validation_failed",
                  "data",
                  Map.of(
                      "errors",
                      Map.of(
                          "date",
                          "Missing or invalid date parameter. Format must be YYYY-MM-DD."))));
    }
  }

  @GetMapping("/monthly")
  public ResponseEntity<?> monthly(@CurrentUser Long userId, @RequestParam String month) {
    try {
      YearMonth ym = YearMonth.parse(month);

      YearMonth prevMonth = ym.minusMonths(1);
      YearMonth nextMonth = ym.plusMonths(1);

      LocalDate startDate = prevMonth.atDay(1);
      LocalDate endDate = nextMonth.atEndOfMonth();

      Map<LocalDate, List<ScheduleResponse>> allSchedules =
          scheduleService.getSchedulesByRange(userId, startDate, endDate);

      // 모든 일정 평탄화
      List<ScheduleResponse> flattenedSchedules =
          allSchedules.values().stream().flatMap(List::stream).collect(Collectors.toList());

      return ResponseEntity.ok(ApiResponse.success("monthly_schedule", flattenedSchedules));
    } catch (Exception e) {
      System.out.println("e = " + e);
      return ResponseEntity.badRequest()
          .body(
              Map.of(
                  "message",
                  "validation_failed",
                  "data",
                  Map.of(
                      "errors",
                      Map.of(
                          "date", "Missing or invalid month parameter. Format must be YYYY-MM."))));
    }
  }

  @GetMapping("/yearly")
  public ResponseEntity<?> yearly(@CurrentUser Long userId, @RequestParam String year) {
    try {
      int y = Integer.parseInt(year);

      Map<LocalDate, List<ScheduleResponse>> allSchedules =
          scheduleService.getSchedulesByRange(
              userId, LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31));

      List<ScheduleResponse> flattenedSchedules =
          allSchedules.values().stream().flatMap(List::stream).collect(Collectors.toList());
      return ResponseEntity.ok(ApiResponse.success("yearly_schedule", flattenedSchedules));
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body(
              Map.of(
                  "message",
                  "validation_failed",
                  "data",
                  Map.of(
                      "errors",
                      Map.of("date", "Missing or invalid year parameter. Format must be YYYY."))));
    }
  }
}
