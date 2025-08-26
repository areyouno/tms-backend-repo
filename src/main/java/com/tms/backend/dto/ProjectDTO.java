package com.tms.backend.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record ProjectDTO (
    Long id,
    String name,
    String createdBy,
    LocalDateTime createDate,
    String status,
    LocalDateTime dueDate,
    String sourceLang,
    Set<String> targetLang,
    Long machineTranslationId,
    String ownerUid,
    Long businessUnitId,
    String purchaseOrder,
    String type,
    Long clientId,
    String note,
    Long costCenterId,
    Long domainId,
    Long subdomainId,
    Set<Long> workflowSteps,
    Boolean fileHandover
) {}
