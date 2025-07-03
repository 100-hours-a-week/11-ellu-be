package com.ellu.looper.schedule.repository;

import com.ellu.looper.schedule.entity.Schedule;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

  @Query(
      "SELECT s FROM Schedule s "
          + "WHERE s.user.id = :userId "
          + "AND s.deletedAt IS NULL "
          + "AND s.startTime < :end "
          + "AND s.endTime >= :start")
  List<Schedule> findDailySchedules(
      @Param("userId") Long userId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  @Query(
      "SELECT s FROM Schedule s "
          + "WHERE s.user.id = :userId "
          + "AND s.deletedAt IS NULL "
          + "AND s.startTime <=:end "
          + "AND s.endTime>= :start")
  List<Schedule> findSchedulesBetween(
      @Param("userId") Long userId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  Optional<Schedule> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);
}
