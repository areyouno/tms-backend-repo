package com.tms.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record JobDTO(
    Long id,
    String sourceLang,
    Set<String> targetLangs,
    String jobOwnerUid,
    String jobOwnerName,
    String fileName,
    Long fileSize,
    String filePathOriginal,
    String filePathConverted,
    String filePathTranslated,
    String filePathTarget,
    String contentType,
    Long projectId,
    List<JobWorkflowStepDTO> workflowSteps,
    Long segmentCount,
    Long pageCount,
    Long wordCount,
    Long characterCount,
    Long progress,
    LocalDateTime createDate
) 
{}
