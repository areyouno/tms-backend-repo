package com.tms.backend.dto;

public record ImportTmxRequestDTO(
        String userName,
        boolean overwrite,
        String jobId
) {}
