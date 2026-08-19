package com.tms.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

// One row per workflow step of a Job (jobs with no workflow steps produce a single row).
// Backs the flattened, paginated GET /api/jobs endpoint.
public record JobRowDTO(
    Long id,
    Long workflowStepId,
    String fileName,
    String project,
    Integer stepOrder,
    String workflowStepName,
    String status,
    String stepStatus,
    String jobOwnerName,
    String sourceLang,
    List<String> targetLangs,
    Long wordCount,
    LocalDateTime createDate,
    LocalDateTime dueDate,
    Long confirmedPercentage,
    String providerName,
    String providerUid
) {}
