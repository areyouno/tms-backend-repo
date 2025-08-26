package com.tms.backend.workflowSteps;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowStepsRepository extends JpaRepository<WorkflowSteps, Long> {
    
}
