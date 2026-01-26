package com.tms.backend.projectTbAssignment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTbAssignmentRepository extends JpaRepository<ProjectTbAssignment, Long> {
    List<ProjectTbAssignment> findByProjectId(Long projectId);

    Optional<ProjectTbAssignment> findByProjectIdAndTbId(Long projectId, Long tbId);
}
