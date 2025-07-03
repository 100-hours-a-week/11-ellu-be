package com.ellu.looper.project.repository;

import com.ellu.looper.project.entity.Project;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

  Optional<Project> findByIdAndDeletedAtIsNull(Long projectId);
}
