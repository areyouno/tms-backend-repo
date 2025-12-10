package com.tms.backend.dto;

import java.util.Set;

import com.tms.backend.project.ProjectAutomationRule;

public record ProjectTemplateCreateDTO(
    String name,
    String projectName,
    Long userId,
    Long ownerId,
    String sourceLang,
    Set<String> targetLang,
    Long machineTranslationId,
    Long businessUnitId,
    String type,
    Long clientId,
    Long costCenterId,
    Long domainId,
    Long subdomainId,
    Long vendorId,
    Set<Long> workflowSteps,
    Set<ProjectAutomationRule> enabledRules,
    String note,
    Long createdBy
) {}
