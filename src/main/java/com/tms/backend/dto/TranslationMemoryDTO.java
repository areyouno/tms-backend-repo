package com.tms.backend.dto;

import java.time.LocalDateTime;

public record TranslationMemoryDTO(
    String name,
    String createdBy,
    LocalDateTime createDate,
    String ownerName,
    String sourceLang,
    String targetLang,
    Long segmentsCount
) {}
