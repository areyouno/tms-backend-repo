package com.tms.backend.dto;

import java.time.LocalDateTime;

import com.tms.backend.job.JobWorkflowStatus;

public record JobWorkflowStepEditDTO(
    Long id,
    String providerUid,
    JobWorkflowStatus status,
    LocalDateTime dueDate
) {}
