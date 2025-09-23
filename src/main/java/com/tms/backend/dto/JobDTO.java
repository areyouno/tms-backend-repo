package com.tms.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record JobDTO(
    Long id,
    String sourceLang,
    Set<String> targetLangs,
    LocalDateTime dueDate,
    String jobOwnerUid,
    String jobOwnerName,
    String fileName,
    Long fileSize,
    String filePath,
    String contentType,
    Long projectId,
    List<JobWorkflowStepDTO> workflowSteps,
    Long wordCount,
    Long progress,
    LocalDateTime createDate
) 
{}
