package com.tms.backend.job;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobWorkflowStepRepository extends JpaRepository<JobWorkflowStep, Long>{
}
