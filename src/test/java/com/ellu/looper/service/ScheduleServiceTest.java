package com.ellu.looper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ellu.looper.dto.schedule.ProjectScheduleResponse;
import com.ellu.looper.dto.schedule.ScheduleCreateRequest;
import com.ellu.looper.dto.schedule.ScheduleResponse;
import com.ellu.looper.dto.schedule.ScheduleUpdateRequest;
import com.ellu.looper.entity.Project;
import com.ellu.looper.entity.Schedule;
import com.ellu.looper.entity.User;
import com.ellu.looper.exception.ValidationException;
import com.ellu.looper.repository.ProjectRepository;
import com.ellu.looper.repository.ScheduleRepository;
import com.ellu.looper.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

  @Mock private ScheduleRepository scheduleRepository;

  @Mock private UserRepository memberRepository;

  @Mock private ProjectRepository projectRepository;

  @Mock private ProjectScheduleService projectScheduleService;

  @InjectMocks private ScheduleService scheduleService;

  private User testUser;
  private Schedule testSchedule;
  private LocalDateTime now;
  private LocalDateTime future;

  @BeforeEach
  void setUp() {
    now = LocalDateTime.now();
    future = now.plusHours(1);

    testUser = User.builder().id(1L).nickname("testUser").build();

    testSchedule =
        Schedule.builder()
            .id(1L)
            .user(testUser)
            .title("Test Schedule")
            .description("Test Description")
            .startTime(now)
            .endTime(future)
            .isCompleted(false)
            .isAiRecommended(false)
            .build();
  }

  @Test
  @DisplayName("일정 생성 성공")
  void createSchedule_Success() {
    // given
    ScheduleCreateRequest request =
        new ScheduleCreateRequest("New Schedule", "New Description", false, now, future);
    when(memberRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);

    // when
    ScheduleResponse response = scheduleService.createSchedule(1L, request);

    // then
    assertThat(response).isNotNull();
    assertThat(response.title()).isEqualTo("Test Schedule");
    assertThat(response.startTime()).isEqualTo(now);
    assertThat(response.endTime()).isEqualTo(future);
    verify(scheduleRepository).save(any(Schedule.class));
  }

  @Test
  @DisplayName("일정 생성 실패 - 유효하지 않은 시간")
  void createSchedule_InvalidTime() {
    // given
    ScheduleCreateRequest request =
        new ScheduleCreateRequest(
            "New Schedule", "New Description", false, future, now // end time is before start time
            );
    when(memberRepository.findById(1L)).thenReturn(Optional.of(testUser));

    // when & then
    assertThatThrownBy(() -> scheduleService.createSchedule(1L, request))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  @DisplayName("일정 수정 성공")
  void updateSchedule_Success() {
    // given
    ScheduleUpdateRequest request =
        new ScheduleUpdateRequest(
            "Updated Title", "Updated Description", true, now.plusHours(2), future.plusHours(2));
    when(scheduleRepository.findByIdAndUserIdAndDeletedAtIsNull(1L, 1L))
        .thenReturn(Optional.of(testSchedule));
    when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);

    // when
    ScheduleResponse response = scheduleService.updateSchedule(1L, 1L, request);

    // then
    assertThat(response).isNotNull();
    verify(scheduleRepository).save(any(Schedule.class));
  }

  @Test
  @DisplayName("일정 삭제 성공")
  void deleteSchedule_Success() {
    // given
    when(scheduleRepository.findByIdAndUserIdAndDeletedAtIsNull(1L, 1L))
        .thenReturn(Optional.of(testSchedule));

    // when
    scheduleService.deleteSchedule(1L, 1L);

    // then
    verify(scheduleRepository).delete(testSchedule);
  }

  @Test
  @DisplayName("일일 일정 조회 성공")
  void getDailySchedules_Success() {
    // given
    LocalDate today = LocalDate.now();
    List<Schedule> schedules = List.of(testSchedule);
    List<ProjectScheduleResponse> projectSchedules =
        List.of(
            new ProjectScheduleResponse(
                2L, "Project Schedule", "Description", now, future, false, true, null));

    when(scheduleRepository.findDailySchedules(
            1L, today.atStartOfDay(), today.plusDays(1).atStartOfDay()))
        .thenReturn(schedules);
    when(projectRepository.findAllByMemberId(1L)).thenReturn(List.of(new Project()));
    when(projectScheduleService.getDailySchedules(any(), any())).thenReturn(projectSchedules);

    // when
    List<ScheduleResponse> response = scheduleService.getDailySchedules(1L, today);

    // then
    assertThat(response).isNotEmpty();
    assertThat(response.size()).isEqualTo(2); // personal + project schedules
  }

  @Test
  @DisplayName("프로젝트 일정 범위 조회 성공")
  void getSchedulesByRange_Success() {
    // given
    Long memberId = 1L;
    LocalDate startDate = LocalDate.of(2025, 5, 1);
    LocalDate endDate = LocalDate.of(2025, 5, 2);

    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusNanos(1);

    Schedule personalSchedule =
        Schedule.builder()
            .id(101L)
            .title("개인 일정")
            .description("설명")
            .isCompleted(false)
            .isAiRecommended(false)
            .startTime(LocalDateTime.of(2025, 5, 1, 9, 0))
            .endTime(LocalDateTime.of(2025, 5, 1, 10, 0))
            .build();

    // 개인 일정 반환
    when(scheduleRepository.findSchedulesBetween(memberId, start, end))
        .thenReturn(List.of(personalSchedule));

    Project project = Project.builder().id(300L).build();
    when(projectRepository.findAllByMemberId(memberId)).thenReturn(List.of(project));

    // project 일정은 ScheduleResponse로 리턴됨
    ProjectScheduleResponse projectSchedule =
        new ProjectScheduleResponse(
            202L,
            "프로젝트 일정",
            "설명",
            LocalDateTime.of(2025, 5, 2, 14, 0),
            LocalDateTime.of(2025, 5, 2, 15, 0),
            true,
            true,
            null);

    when(projectScheduleService.getSchedulesByRange(eq(300L), any(), any()))
        .thenReturn(Map.of("2025-05-02", List.of(projectSchedule)));

    // when
    Map<LocalDate, List<ScheduleResponse>> result =
        scheduleService.getSchedulesByRange(memberId, startDate, endDate);

    // then
    assertThat(result).hasSize(2);
    assertThat(result).containsKeys(LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 2));

    List<ScheduleResponse> day1 = result.get(LocalDate.of(2025, 5, 1));
    assertThat(day1).hasSize(1);
    assertThat(day1.get(0).title()).isEqualTo("개인 일정");

    List<ScheduleResponse> day2 = result.get(LocalDate.of(2025, 5, 2));
    assertThat(day2).hasSize(1);
    assertThat(day2.get(0).title()).isEqualTo("프로젝트 일정");
  }

  @Test
  @DisplayName("존재하지 않는 사용자로 일정 생성 시도 시 예외 발생")
  void createSchedule_UserNotFound() {
    // given
    ScheduleCreateRequest request =
        new ScheduleCreateRequest("New Schedule", "New Description", false, now, future);
    when(memberRepository.findById(999L)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> scheduleService.createSchedule(999L, request))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("unauthorized");
  }
}
