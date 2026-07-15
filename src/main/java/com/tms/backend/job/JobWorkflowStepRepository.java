package com.tms.backend.job;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobWorkflowStepRepository extends JpaRepository<JobWorkflowStep, Long>{
    List<JobWorkflowStep> findByJob_IdInAndWorkflowStep_Id(List<Long> jobIds, Long workflowStepId);
}
