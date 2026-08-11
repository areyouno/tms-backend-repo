package com.tms.backend.jobWorkflowStepTmxCopy;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobWorkflowStepTmxCopyRepository
        extends JpaRepository<JobWorkflowStepTmxCopy, Long> {

    List<JobWorkflowStepTmxCopy> findByJobWorkflowStepId(Long jobWorkflowStepId);

    List<JobWorkflowStepTmxCopy> findByProjectTmAssignmentId(Long projectTmAssignmentId);

    List<JobWorkflowStepTmxCopy> findByJobWorkflowStep_Job_IdAndJobWorkflowStep_WorkflowStep_Id(
        Long jobId,
        Long workflowStepId
    );

    boolean existsByJobWorkflowStepIdAndProjectTmAssignmentId(
        Long jobWorkflowStepId,
        Long projectTmAssignmentId
    );
}
