package com.ellu.looper.service;

import com.ellu.looper.schedule.dto.ProjectScheduleCreateRequest;
import com.ellu.looper.schedule.dto.ProjectScheduleCreateRequest.ProjectScheduleDto;
import com.ellu.looper.schedule.dto.ProjectScheduleResponse;
import com.ellu.looper.schedule.dto.ProjectScheduleUpdateRequest;
import com.ellu.looper.project.entity.Project;
import com.ellu.looper.schedule.entity.ProjectSchedule;
import com.ellu.looper.schedule.service.ProjectScheduleService;
import com.ellu.looper.user.entity.User;
import com.ellu.looper.exception.ValidationException;
import com.ellu.looper.project.repository.ProjectRepository;
import com.ellu.looper.schedule.repository.ProjectScheduleRepository;
import com.ellu.looper.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectScheduleServiceTest {

  @Mock
  private ProjectScheduleRepository scheduleRepository;
  @Mock
  private ProjectRepository projectRepository;
  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private ProjectScheduleService service;

  private Project project;
  private User user;
  private ProjectSchedule schedule;
  private LocalDateTime now;
  private LocalDateTime future;

  @BeforeEach
  void setUp() {
    now = LocalDateTime.now();
    future = now.plusHours(1);

    project = Project.builder().id(1L).build();
    user = User.builder().id(2L).build();
    schedule = ProjectSchedule.builder()
        .project(project)
        .user(user)
        .title("title")
        .description("desc")
        .startTime(now)
        .endTime(future)
        .isCompleted(false)
        .build();
  }

  @Test
  @DisplayName("프로젝트 일정 생성 성공")
  void createSchedules_Success() {
    ProjectScheduleDto dto = new ProjectScheduleDto("title", "desc", now, future, false);
    ProjectScheduleCreateRequest req = new ProjectScheduleCreateRequest(List.of(dto));

    when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
    when(userRepository.findById(2L)).thenReturn(Optional.of(user));
    when(scheduleRepository.save(any())).thenReturn(schedule);

    List<ProjectScheduleResponse> result = service.createSchedules(1L, 2L, req);

    assertThat(result).isNotEmpty();
    assertThat(result.get(0).title()).isEqualTo("title");
    verify(scheduleRepository).save(any());
  }

  @Test
  @DisplayName("프로젝트 일정 생성 실패 - 유효하지 않은 시간")
  void createSchedules_InvalidTime() {
    ProjectScheduleDto dto = new ProjectScheduleDto("New Schedule", "New Description", future, now,
        false);
    ProjectScheduleCreateRequest req = new ProjectScheduleCreateRequest(List.of(dto));

    when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
    when(userRepository.findById(2L)).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> service.createSchedules(1L, 2L, req))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  @DisplayName("프로젝트 일정 수정 성공")
  void updateSchedule_Success() {
    // given
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime future = now.plusDays(1);

    ProjectScheduleUpdateRequest req = new ProjectScheduleUpdateRequest(
        "Updated Title", "Updated Description",
        now.plusHours(2), future.plusHours(2), true);

    User mockUser = mock(User.class);
    when(mockUser.getId()).thenReturn(2L);

    ProjectSchedule schedule = mock(ProjectSchedule.class);
    when(schedule.getUser()).thenReturn(mockUser);
    when(schedule.getId()).thenReturn(10L);
    when(schedule.getTitle()).thenReturn("Updated Title");
    when(schedule.getDescription()).thenReturn("Updated Description");
    when(schedule.getStartTime()).thenReturn(now.plusHours(2));
    when(schedule.getEndTime()).thenReturn(future.plusHours(2));
    when(schedule.isCompleted()).thenReturn(true);
    when(schedule.getProject()).thenReturn(project);

    when(scheduleRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(schedule));

    // when
    ProjectScheduleResponse resp = service.updateSchedule(10L, 2L, req);

    // then
    assertThat(resp).isNotNull();
    assertThat(resp.id()).isEqualTo(10L);
    assertThat(resp.title()).isEqualTo("Updated Title");
    assertThat(resp.description()).isEqualTo("Updated Description");
    assertThat(resp.start_time()).isEqualTo(now.plusHours(2));
    assertThat(resp.end_time()).isEqualTo(future.plusHours(2));
    assertThat(resp.is_completed()).isTrue();

    verify(schedule).update(
        "Updated Title", "Updated Description", now.plusHours(2), future.plusHours(2), true);
  }


  @Test
  @DisplayName("프로젝트 일정 수정 실패 - 권한 없음")
  void updateSchedule_Unauthorized() {
    User otherUser = User.builder().id(99L).build();
    ProjectSchedule otherSchedule = ProjectSchedule.builder().user(otherUser).build();
    ProjectScheduleUpdateRequest req = new ProjectScheduleUpdateRequest("title", "description",
        LocalDateTime.now(), LocalDateTime.now().plusHours(1), false);

    when(scheduleRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.of(otherSchedule));

    assertThatThrownBy(() -> service.updateSchedule(11L, 2L, req))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Unauthorized");
  }

  @Test
  @DisplayName("프로젝트 일정 삭제 성공")
  void deleteSchedule_Success() {
    when(scheduleRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(schedule));
    when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
    service.deleteSchedule(10L, 2L);

    assertThat(scheduleRepository.findById(10L).get().getDeletedAt()).isNotNull();
  }

  @Test
  @DisplayName("일일 프로젝트 일정 조회 성공")
  void getDailySchedules_Success() {
    LocalDate today = LocalDate.now();
    when(scheduleRepository.findDailyProjectSchedules(eq(1L), any(), any()))
        .thenReturn(List.of(schedule));

    List<ProjectScheduleResponse> result = service.getDailySchedules(1L, today);

    assertThat(result).isNotEmpty();
    assertThat(result.get(0).title()).isEqualTo("title");
  }

  @Test
  @DisplayName("주간 프로젝트 일정 조회 성공")
  void getWeeklySchedules_Success() {
    LocalDate startDate = LocalDate.now();
    when(projectRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(project));
    when(scheduleRepository.findSchedulesBetween(eq(1L), any(), any()))
        .thenReturn(List.of(schedule));

    Map<String, List<ProjectScheduleResponse>> result = service.getWeeklySchedules(1L, startDate);

    assertThat(result).isNotEmpty();
  }

  @Test
  @DisplayName("월간 프로젝트 일정 조회 성공")
  void getMonthlySchedules_Success() {
    YearMonth month = YearMonth.now();
    when(projectRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(project));
    when(scheduleRepository.findSchedulesBetween(eq(1L), any(), any()))
        .thenReturn(List.of(schedule));

    Map<String, List<ProjectScheduleResponse>> result = service.getMonthlySchedules(1L, month);

    assertThat(result).isNotEmpty();
  }

  @Test
  @DisplayName("연간 프로젝트 일정 조회 성공")
  void getYearlySchedules_Success() {
    Year year = Year.now();
    when(projectRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(project));
    when(scheduleRepository.findSchedulesBetween(eq(1L), any(), any()))
        .thenReturn(List.of(schedule));

    Map<String, List<ProjectScheduleResponse>> result = service.getYearlySchedules(1L, year);

    assertThat(result).isNotEmpty();
  }

  @Test
  @DisplayName("프로젝트 일정 범위 조회 성공")
  void getSchedulesByRange_Success() {
    // given
    Long projectId = 1L;
    LocalDateTime start = LocalDateTime.of(2025, 5, 1, 0, 0);
    LocalDateTime end = LocalDateTime.of(2025, 5, 31, 23, 59);

    Project project = mock(Project.class);
    when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));

    ProjectSchedule schedule1 = mock(ProjectSchedule.class);
    when(schedule1.getId()).thenReturn(100L);
    when(schedule1.getTitle()).thenReturn("Task A");
    when(schedule1.getDescription()).thenReturn("Desc A");
    when(schedule1.getStartTime()).thenReturn(LocalDateTime.of(2025, 5, 10, 10, 0));
    when(schedule1.getEndTime()).thenReturn(LocalDateTime.of(2025, 5, 10, 12, 0));
    when(schedule1.isCompleted()).thenReturn(false);
    when(schedule1.getProject()).thenReturn(project);

    ProjectSchedule schedule2 = mock(ProjectSchedule.class);
    when(schedule2.getId()).thenReturn(101L);
    when(schedule2.getTitle()).thenReturn("Task B");
    when(schedule2.getDescription()).thenReturn("Desc B");
    when(schedule2.getStartTime()).thenReturn(LocalDateTime.of(2025, 5, 10, 14, 0));
    when(schedule2.getEndTime()).thenReturn(LocalDateTime.of(2025, 5, 10, 16, 0));
    when(schedule2.isCompleted()).thenReturn(true);
    when(schedule2.getProject()).thenReturn(project);

    ProjectSchedule schedule3 = mock(ProjectSchedule.class);
    when(schedule3.getId()).thenReturn(102L);
    when(schedule3.getTitle()).thenReturn("Task C");
    when(schedule3.getDescription()).thenReturn("Desc C");
    when(schedule3.getStartTime()).thenReturn(LocalDateTime.of(2025, 5, 11, 9, 0));
    when(schedule3.getEndTime()).thenReturn(LocalDateTime.of(2025, 5, 11, 10, 0));
    when(schedule3.isCompleted()).thenReturn(false);
    when(schedule3.getProject()).thenReturn(project);

    List<ProjectSchedule> schedules = List.of(schedule1, schedule2, schedule3);
    when(scheduleRepository.findSchedulesBetween(projectId, start, end)).thenReturn(schedules);

    // when
    Map<String, List<ProjectScheduleResponse>> result =
        service.getSchedulesByRange(projectId, start, end);

    // then
    assertThat(result).hasSize(2);
    assertThat(result).containsKeys("2025-05-10", "2025-05-11");

    List<ProjectScheduleResponse> may10 = result.get("2025-05-10");
    assertThat(may10).hasSize(2);
    assertThat(may10).extracting("title").containsExactlyInAnyOrder("Task A", "Task B");

    List<ProjectScheduleResponse> may11 = result.get("2025-05-11");
    assertThat(may11).hasSize(1);
    assertThat(may11.get(0).title()).isEqualTo("Task C");
  }

}