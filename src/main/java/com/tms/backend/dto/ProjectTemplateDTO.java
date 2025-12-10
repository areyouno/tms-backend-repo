package com.tms.backend.dto;

import java.util.Set;

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
    ReferenceDTO createdBy
) {}
