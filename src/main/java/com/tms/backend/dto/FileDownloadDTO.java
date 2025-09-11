package com.tms.backend.dto;

public record FileDownloadDTO(
    String filename,
    String contentType,
    String filePath
) {}
