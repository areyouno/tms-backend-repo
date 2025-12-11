package com.tms.backend.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tms.backend.project.ProjectAutomationRule;

public record ProjectTemplateDTO(
    Long id,
    String name,
    String projectName,
    Long userId,
    ReferenceDTO owner,
    String sourceLang,
    Set<String> targetLang,
    Long machineTranslationId,
    ReferenceDTO businessUnit,
    String type,
    ReferenceDTO client,
    ReferenceDTO costCenterId,
    ReferenceDTO domainId,
    ReferenceDTO subdomainId,
    ReferenceDTO vendor,
    Set<Long> workflowSteps,
    Set<ProjectAutomationRule> enabledRules,
    String note,
    ReferenceDTO createdBy,
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDateTime createdDate
) {}
