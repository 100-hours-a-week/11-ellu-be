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

  @Query(
      "SELECT COUNT(s) FROM Schedule s " + "WHERE s.user.id = :userId " + "AND s.deletedAt IS NULL")
  Long countByUserIdAndDeletedAtIsNull(@Param("userId") Long userId);

  @Query(
      "SELECT COUNT(s) FROM Schedule s "
          + "WHERE s.user.id = :userId "
          + "AND s.deletedAt IS NULL "
          + "AND s.isCompleted = true")
  Long countCompletedByUserIdAndDeletedAtIsNull(@Param("userId") Long userId);

  @Query(
      "SELECT DATE(s.createdAt) as date, COUNT(s) as count "
          + "FROM Schedule s "
          + "WHERE s.user.id = :userId "
          + "AND s.deletedAt IS NULL "
          + "AND s.createdAt >= :startDate "
          + "AND s.createdAt < :endDate "
          + "GROUP BY DATE(s.createdAt)")
  List<Object[]> countSchedulesByDateRange(
      @Param("userId") Long userId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  @Query(
      "SELECT s.plan.id, COUNT(s) as total, COUNT(CASE WHEN s.isCompleted = true THEN 1 END) as completed "
          + "FROM Schedule s "
          + "WHERE s.user.id = :userId "
          + "AND s.deletedAt IS NULL "
          + "AND s.plan IS NOT NULL "
          + "GROUP BY s.plan.id")
  List<Object[]> getPlanAchievementStats(@Param("userId") Long userId);

  @Query(
      "SELECT s FROM Schedule s "
          + "WHERE s.user.id = :userId "
          + "AND s.deletedAt IS NULL "
          + "AND s.startTime <=:end "
          + "AND s.endTime>= :start "
          + "AND (s.title LIKE CONCAT('%', :keyword, '%') "
          + "OR s.description LIKE CONCAT('%', :keyword, '%'))")

  //  AND (s.title LIKE CONCAT('%', :keyword, '%') "
  //      + "OR s.description LIKE CONCAT('%', :keyword, '%'))
  List<Schedule> findRelatedSchedules(
      @Param("userId") Long userId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("keyword") String keyword);

  @Query(
      "SELECT s FROM Schedule s "
          + "WHERE s.plan.id = :planId "
          + "AND s.deletedAt IS NULL "
          + "AND s.startTime <=:end "
          + "AND s.endTime>= :start")
  List<Schedule> findPlanSchedules(
      @Param("planId") Long planId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);
}
