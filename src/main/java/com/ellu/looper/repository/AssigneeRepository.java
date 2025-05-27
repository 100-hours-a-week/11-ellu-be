package com.ellu.looper.repository;

import com.ellu.looper.project.entity.Assignee;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssigneeRepository extends JpaRepository<Assignee, Long> {

  List<Assignee> findByProjectScheduleIdAndDeletedAtIsNull(Long scheduleId);
}
