package com.tms.backend.dto;

import java.time.LocalDateTime;

import com.tms.backend.job.JobWorkflowStatus;

public record JobWorkflowStepDTO(
    Long id,
    Long workflowStepId,
    String workflowStepName,
    String providerUid,
    LocalDateTime dueDate,
    String notifyUserUid,
    JobWorkflowStatus status
) {}
