package com.tms.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmxImportJobStatusDTO(
        String jobId,
        Long tmId,
        String fileName,
        Integer totalCount,
        Integer processedCount,
        Integer importedCount,
        Integer skippedCount,
        Integer overwrittenCount,
        Double progressPercent,
        @JsonProperty("isCompleted") boolean isCompleted,
        String errorMessage,
        String startedAt,
        String completedAt
) {}
