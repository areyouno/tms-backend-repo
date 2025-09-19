package com.tms.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.tms.backend.job.JobWorkflowStatus;

public record JobDTO(
    Long id,
    JobWorkflowStatus status,
    String sourceLang,
    Set<String> targetLangs,
    Long providerId,
    LocalDateTime dueDate,
    Long jobOwnerId,
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
