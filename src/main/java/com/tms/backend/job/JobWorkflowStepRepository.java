package com.tms.backend.job;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobWorkflowStepRepository extends JpaRepository<JobWorkflowStep, Long>{
    List<JobWorkflowStep> findByJob_IdInAndWorkflowStep_Id(List<Long> jobIds, Long workflowStepId);

    List<JobWorkflowStep> findByWorkflowStep_IdAndJob_Project_IdAndJob_SourceLangAndJob_DeletedFalse(
        Long workflowStepId,
        Long projectId,
        String sourceLang
    );

    boolean existsByProviderId(Long userId);

    boolean existsByNotifyUserId(Long userId);
}
