package com.ellu.looper.schedule.repository;

import com.ellu.looper.project.entity.Project;
import com.ellu.looper.schedule.entity.ProjectSchedule;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectScheduleRepository extends JpaRepository<ProjectSchedule, Long> {

  Optional<ProjectSchedule> findByIdAndDeletedAtIsNull(Long id);

  @Query(
      "SELECT s FROM ProjectSchedule s WHERE s.project.id = :projectId AND s.deletedAt IS NULL "
          + "AND s.startTime < :end AND s.endTime >= :start")
  List<ProjectSchedule> findDailyProjectSchedules(
      @Param("projectId") Long projectId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  @Query(
      "SELECT s FROM ProjectSchedule s WHERE s.project.id = :projectId AND s.deletedAt IS NULL AND s.startTime <=:end AND s.endTime>= :start")
  List<ProjectSchedule> findSchedulesBetween(
      @Param("projectId") Long projectId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  List<ProjectSchedule> findByProjectAndDeletedAtIsNull(Project project);

  @Query(
      "SELECT ps FROM ProjectSchedule ps "
          + "JOIN FETCH ps.project "
          + "LEFT JOIN FETCH ps.assignees a "
          + "LEFT JOIN FETCH a.user "
          + "WHERE ps.id = :id AND ps.deletedAt IS NULL")
  Optional<ProjectSchedule> findWithDetailsById(@Param("id") Long id);
}
