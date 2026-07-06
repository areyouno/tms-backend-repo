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
        Integer languageMismatchSkippedCount,
        Integer emptySegmentSkippedCount,
        Integer duplicateSkippedCount,
        Integer invalidTuSkippedCount,
        Integer overwrittenCount,
        Double progressPercent,
        @JsonProperty("isCompleted") boolean isCompleted,
        @JsonProperty("isCancelled") boolean isCancelled,
        boolean wasDiscarded,
        Integer discardedCount,
        boolean keepPartialResults,
        String errorMessage,
        String startedAt,
        String completedAt
) {}
