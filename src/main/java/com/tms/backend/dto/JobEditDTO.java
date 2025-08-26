package com.tms.backend.dto;

import java.time.LocalDateTime;

public record JobEditDTO(
    String provider,
    String status,
    LocalDateTime dueDate
) {}
