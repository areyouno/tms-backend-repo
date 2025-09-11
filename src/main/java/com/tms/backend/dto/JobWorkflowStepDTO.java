package com.tms.backend.dto;

import java.time.LocalDateTime;

import com.tms.backend.job.JobWorkflowStatus;

public record JobWorkflowStepDTO(
    Long id,
    Long workflowStepId,
    String workflowStepName,
    Long providerId,
    LocalDateTime dueDate,
    Long notifyUserId,
    JobWorkflowStatus status,
    Integer stepOrder
) {}
