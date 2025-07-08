package com.ellu.looper.schedule.service;

import com.ellu.looper.schedule.dto.DailyAchievementResponse;
import com.ellu.looper.schedule.dto.PersonalScheduleAchievementResponse;
import com.ellu.looper.schedule.dto.PlanAchievementResponse;
import com.ellu.looper.schedule.entity.Plan;
import com.ellu.looper.schedule.repository.PlanRepository;
import com.ellu.looper.schedule.repository.ScheduleRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AchievementService {

  private final ScheduleRepository scheduleRepository;
  private final PlanRepository planRepository;

  // 지난 3달 간 일 단위로 생성한 일정의 개수 조회
  public List<DailyAchievementResponse> getDailyAchievement(Long userId) {
    List<DailyAchievementResponse> response = new ArrayList<>();
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = endDate.minusMonths(3);

    List<Object[]> dailyCounts =
        scheduleRepository.countSchedulesByDateRange(
            userId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

    // DB에서 일별 카운트 조회한 결과를 Map으로 변환
    Map<LocalDate, Long> countMap = new HashMap<>();
    for (Object[] result : dailyCounts) {
      LocalDate date = ((java.sql.Date) result[0]).toLocalDate();
      Long count = ((Number) result[1]).longValue();
      countMap.put(date, count);
    }

    // 3달 간의 모든 날짜에 대해 응답 생성 (0개인 날도 포함)
    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      Long count = countMap.getOrDefault(date, 0L);
      response.add(
          DailyAchievementResponse.builder()
              .date(date.atStartOfDay())
              .created_schedules(count)
              .build());
    }

    return response;
  }

  // 개인 스케줄 달성률 조회
  public PersonalScheduleAchievementResponse getPersonalScheduleAchievement(Long userId) {
    Long totalSchedules = scheduleRepository.countByUserIdAndDeletedAtIsNull(userId);
    Long completedSchedules = scheduleRepository.countCompletedByUserIdAndDeletedAtIsNull(userId);

    double achievementRate =
        (totalSchedules > 0) ? (double) completedSchedules / totalSchedules * 100.0 : 0.0;

    return new PersonalScheduleAchievementResponse(
        totalSchedules, completedSchedules, achievementRate);
  }

  // 개인 스케줄 중에서 plan별로 달성률 조회
  public List<PlanAchievementResponse> getPlanAchievement(Long userId) {
    List<PlanAchievementResponse> achievementResponses = new ArrayList<>();
    List<Plan> planList = planRepository.findByUserId(userId);
    List<Object[]> planStats = scheduleRepository.getPlanAchievementStats(userId);

    // 결과를 Map으로 변환
    Map<Long, PlanStats> statsMap = new HashMap<>();
    for (Object[] result : planStats) {
      Long planId = (Long) result[0];
      Long total = ((Number) result[1]).longValue();
      Long completed = ((Number) result[2]).longValue();
      statsMap.put(planId, new PlanStats(total, completed));
    }

    for (Plan plan : planList) {
      PlanStats stats = statsMap.getOrDefault(plan.getId(), new PlanStats(0L, 0L));
      double achievementRate =
          (stats.total > 0) ? (double) stats.completed / stats.total * 100.0 : 0.0;

      achievementResponses.add(
          PlanAchievementResponse.builder()
              .title(plan.getTitle())
              .total_schedules(stats.total)
              .achieved_schedules(stats.completed)
              .achievement_rate(achievementRate)
              .build());
    }

    return achievementResponses;
  }

  // Plan 통계 정보
  private static class PlanStats {
    final Long total;
    final Long completed;

    PlanStats(Long total, Long completed) {
      this.total = total;
      this.completed = completed;
    }
  }
}
