package com.tms.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TaskListCreateDTO(
    List<Long> jobIds,
    Long targetLangId,
    Long workflowStepId,
    String taskName,
    LocalDateTime startDate,
    LocalDateTime dueDate,
    String description,
    String assigneeUid
) {}
