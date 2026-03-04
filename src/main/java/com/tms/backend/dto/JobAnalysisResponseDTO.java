package com.tms.backend.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.tms.backend.jobAnalysis.JobAnalysisType;

public record JobAnalysisResponseDTO(
    Long id,
    String name,
    JobAnalysisType type,
    String sourceLang,
    Set<String> targetLanguages,
    LocalDateTime createDate,
    String createdBy,
    Double netRateRepetition,
    Double netRateContextMatch,
    Double netRatePerfect100,
    Double netRateFuzzy95,
    Double netRateFuzzy85,
    Double netRateFuzzy75,
    Double netRateFuzzy50,
    Double netRateNoMatch,
    Double netRateTotal
) {}
