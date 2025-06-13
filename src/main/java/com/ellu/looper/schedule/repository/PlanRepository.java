package com.ellu.looper.schedule.repository;

import com.ellu.looper.schedule.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {}
