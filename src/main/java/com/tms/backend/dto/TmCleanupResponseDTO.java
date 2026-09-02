package com.tms.backend.dto;

public record TmCleanupResponseDTO(
        String message,
        String fileName,
        Long tmId,
        Integer transUnitCount,
        String cleanedBy
) {}
