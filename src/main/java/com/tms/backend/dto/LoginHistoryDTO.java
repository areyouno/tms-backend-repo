package com.tms.backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record LoginHistoryDTO(
    Long id,
    LocalDate date,
    LocalTime time,
    String ipAddress,
    String userAgent
) {}
