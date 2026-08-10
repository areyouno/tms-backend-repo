package com.tms.backend.dto;

import java.time.LocalDateTime;

public record TmxCopyDTO(
    Long tmId,
    Long workflowStepId,
    String tmxFilePath,
    Long fileSizeBytes,
    LocalDateTime copiedAt
) {}
