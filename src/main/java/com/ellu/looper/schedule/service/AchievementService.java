package com.ellu.looper.schedule.service;

import com.ellu.looper.schedule.dto.DailyAchievementResponse;
import com.ellu.looper.schedule.dto.PersonalScheduleAchievementResponse;
import com.ellu.looper.schedule.dto.PlanAchievementResponse;
import com.ellu.looper.schedule.entity.Plan;
import com.ellu.looper.schedule.entity.Schedule;
import com.ellu.looper.schedule.repository.PlanRepository;
import com.ellu.looper.schedule.repository.ScheduleRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AchievementService {

  private final ScheduleRepository scheduleRepository;
  private final PlanRepository planRepository;

  // 지난 30일간 일 단위로 생성한 일정의 개수 조회
  public List<DailyAchievementResponse> getDailyAchievement(Long userId) {
    List<DailyAchievementResponse> response = new ArrayList<>();
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = endDate.minusDays(29);

    List<Schedule> schedules =
        scheduleRepository.findSchedulesBetween(
            userId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay().minusNanos(1));

    // 일별로 생성된 일정 개수를 계산
    Map<LocalDate, Long> dailyCounts =
        schedules.stream()
            .collect(
                Collectors.groupingBy(
                    schedule -> schedule.getCreatedAt().toLocalDate(), Collectors.counting()));

    // 30일간의 모든 날짜에 대해 응답 생성 (0개인 날도 포함)
    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      Long count = dailyCounts.getOrDefault(date, 0L);
      response.add(
          DailyAchievementResponse.builder()
              .date(date.atStartOfDay())
              .numOfCreatedSchedules(count)
              .build());
    }

    return response;
  }

  // 개인 스케줄 달성률 조회
  public PersonalScheduleAchievementResponse getPersonalScheduleAchievement(Long userId) {
    List<Schedule> userSchedules = scheduleRepository.findByUserIdAndDeletedAtIsNull(userId);
    long completedSchedules = userSchedules.stream().filter(Schedule::isCompleted).count();
    long totalSchedules = userSchedules.size();
    double achievementRate =
        (totalSchedules > 0) ? (double) completedSchedules / totalSchedules * 100.0 : 0.0;

    return new PersonalScheduleAchievementResponse(
        totalSchedules, completedSchedules, achievementRate);
  }

  // 개인 스케줄 중에서 plan별로 달성률 조회
  public List<PlanAchievementResponse> getPlanAchievement(Long userId) {
    List<PlanAchievementResponse> achievementResponses = new ArrayList<>();
    List<Plan> planList = planRepository.findByUserId(userId);
    List<Schedule> userSchedules = scheduleRepository.findByUserIdAndDeletedAtIsNull(userId);
    Map<Long, List<Schedule>> schedulesByPlanId =
        userSchedules.stream()
            .collect(
                Collectors.groupingBy(
                    schedule -> schedule.getPlan() != null ? schedule.getPlan().getId() : null));

    for (Plan plan : planList) {
      List<Schedule> schedulesInPlan =
          schedulesByPlanId.getOrDefault(plan.getId(), new ArrayList<>());
      long totalSchedules = schedulesInPlan.size();
      long achievedSchedules = schedulesInPlan.stream().filter(Schedule::isCompleted).count();

      double achievementRate =
          (totalSchedules > 0) ? (double) achievedSchedules / totalSchedules * 100.0 : 0.0;

      achievementResponses.add(
          PlanAchievementResponse.builder()
              .title(plan.getTitle())
              .totalSchedules(totalSchedules)
              .achievedSchedules(achievedSchedules)
              .achievementRate(achievementRate)
              .build());
    }

    return achievementResponses;
  }
}
