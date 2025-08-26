package com.tms.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public record ProjectSummaryDTO(
    Long id,
    String name,
    LocalDateTime createDate,
    String clientName,
    String ownerName,
    String status,
    LocalDateTime dueDate,
    Set<String> targetLang,
    BigDecimal progress
) {}
