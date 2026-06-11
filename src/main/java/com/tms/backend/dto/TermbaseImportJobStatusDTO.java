package com.tms.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TermbaseImportJobStatusDTO(
        String jobId,
        Long termbaseId,
        String fileName,
        Integer totalCount,
        Integer processedCount,
        Integer importedCount,
        Integer skippedCount,
        Integer failedCount,
        Double progressPercent,
        @JsonProperty("isCompleted") boolean isCompleted,
        @JsonProperty("isCancelled") boolean isCancelled,
        String errorMessage,
        String startedAt,
        String completedAt
) {}
