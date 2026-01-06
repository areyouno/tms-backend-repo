package com.tms.backend.projectTmAssignment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTmAssignmentRepository
        extends JpaRepository<ProjectTmAssignment, Long> {

    List<ProjectTmAssignment> findByProjectId(Long projectId);

    Optional<ProjectTmAssignment>
        findByProjectIdAndTmIdAndWorkflowStepId(
            Long projectId,
            Long tmId,
            Long workflowStepId
        );
}
