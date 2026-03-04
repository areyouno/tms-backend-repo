package com.tms.backend.dto;

public record CompletedFilesNamingSettingDTO(
        String folderName,
        boolean hasNamingRule,
        String namingRule
) {}
