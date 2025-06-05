package com.ellu.looper.schedule.repository;

import com.ellu.looper.schedule.entity.Assignee;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AssigneeRepository extends JpaRepository<Assignee, Long> {

  List<Assignee> findByProjectScheduleIdAndDeletedAtIsNull(Long scheduleId);

  @Query("SELECT a FROM Assignee a " +
      "WHERE a.projectSchedule.project.id = :projectId " +
      "AND a.deletedAt IS NULL")
  List<Assignee> findByProjectIdThroughScheduleAndDeletedAtIsNull(@Param("projectId") Long projectId);

  boolean existsByUserIdAndProjectScheduleIdAndDeletedAtIsNull(Long userId, Long scheduleId);
}
