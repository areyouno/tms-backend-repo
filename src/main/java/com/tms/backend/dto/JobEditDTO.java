package com.tms.backend.dto;

import java.time.LocalDateTime;

import com.tms.backend.job.JobWorkflowStatus;

public record JobEditDTO(
    Long providerId,
    JobWorkflowStatus status,
    LocalDateTime dueDate
) {}
