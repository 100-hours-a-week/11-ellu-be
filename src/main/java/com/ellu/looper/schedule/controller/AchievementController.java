package com.ellu.looper.schedule.controller;

import com.ellu.looper.commons.ApiResponse;
import com.ellu.looper.commons.CurrentUser;
import com.ellu.looper.schedule.dto.DailyAchievementResponse;
import com.ellu.looper.schedule.dto.PersonalScheduleAchievementResponse;
import com.ellu.looper.schedule.dto.PlanAchievementResponse;
import com.ellu.looper.schedule.service.AchievementService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/achievements")
@RequiredArgsConstructor
public class AchievementController {
  private final AchievementService achievementService;

  //  지난 한달동안의 하루 단위 일정 생성한 개수 표시(github 잔디밭 ui처럼)
  @GetMapping("/daily")
  public ResponseEntity<ApiResponse<List<DailyAchievementResponse>>> dailyAchievement(
      @CurrentUser Long userId) {
    List<DailyAchievementResponse> dailyAchievementList =
        achievementService.getDailyAchievement(userId);
    return ResponseEntity.ok(new ApiResponse<>("daily_achievements_fetched", dailyAchievementList));
  }

  //  jira처럼 개인 스케줄 달성률
  @GetMapping("/user/schedules")
  public ResponseEntity<?> personalScheduleAchievement(@CurrentUser Long userId) {
    PersonalScheduleAchievementResponse personalScheduleAchievement =
        achievementService.getPersonalScheduleAchievement(userId);
    return ResponseEntity.ok(
        new ApiResponse<>("personal_schedule_achievement_fetched", personalScheduleAchievement));
  }

  //  개인 스케줄 중에서 plan별로 달성률(챗봇과 대화해서 스케줄을 생성하면 plan단위로 저장이 됨.)
  @GetMapping("/plans")
  public ResponseEntity<?> planAchievement(@CurrentUser Long userId) {
    List<PlanAchievementResponse> planAchievements = achievementService.getPlanAchievement(userId);
    return ResponseEntity.ok(new ApiResponse<>("plan_achievements_fetched", planAchievements));
  }
}
