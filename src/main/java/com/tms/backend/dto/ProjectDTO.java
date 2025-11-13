package com.tms.backend.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record ProjectDTO (
    Long id,
    String name,
    LocalDateTime dueDate,
    String sourceLang,
    Set<String> targetLang,
    Long machineTranslationId,
    Long businessUnitId,
    String purchaseOrder,
    String type,
    Long clientId,
    String note,
    Long costCenterId,
    Long domainId,
    Long subdomainId,
    Set<Long> workflowSteps,
    String ownerUid,
    String createdBy,
    LocalDateTime createDate,
    String status,
    String progress,
    Boolean fileHandover,
    boolean deleted,
    String deletedBy,
    LocalDateTime deletedDate,
    Set<String> automationRules
) {}
