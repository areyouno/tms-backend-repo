package com.tms.backend.dto;

import java.util.Set;

public record ProjectTemplateDTO(
    Long id,
    String name,
    String sourceLang,
    Set<String> targetLang,
    Long machineTranslationId,
    Long businessUnitId,
    String type,
    Long clientId,
    Long costCenterId,
    Long domainId,
    Long subdomainId,
    Set<Long> workflowSteps
) {}
