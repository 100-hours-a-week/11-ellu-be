package com.ellu.looper.schedule.repository;

import com.ellu.looper.schedule.entity.Plan;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {

  List<Plan> findByUserId(Long userId);

  List<Plan> findByUserIdAndCategoryAndCreatedAtAfterAndCreatedAtBefore(
      Long userId, String category, LocalDateTime start, LocalDateTime end);
}
