package com.tms.backend.dto;

import java.time.LocalDate;

public record JobSearchFilterByDate(
    Integer year,
    Integer month,
    LocalDate fromDate,
    LocalDate toDate,
    String period
) {}
