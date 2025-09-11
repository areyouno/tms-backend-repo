package com.tms.backend.dto;

import java.time.LocalDateTime;

public record TranslationMemoryDetailDTO(
    String sourceText,
    String targetText,
    String matchQuality,
    LocalDateTime createDate,
    LocalDateTime lastModifiedDate
) {}
