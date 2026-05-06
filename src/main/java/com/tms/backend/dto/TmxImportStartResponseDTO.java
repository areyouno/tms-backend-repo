package com.tms.backend.dto;

public record TmxImportStartResponseDTO(
        String message,
        String fileName,
        Long fileSize,
        Long tmId,
        String jobId
) {}
