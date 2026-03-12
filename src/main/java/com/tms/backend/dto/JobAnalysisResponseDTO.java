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
    Long repetitionWords,
    Long repetitionSegments,
    Long contextMatchWords,
    Long contextMatchSegments,
    Long perfect100Words,
    Long perfect100Segments,
    Long fuzzy95Words,
    Long fuzzy95Segments,
    Long fuzzy85Words,
    Long fuzzy85Segments,
    Long fuzzy75Words,
    Long fuzzy75Segments,
    Long fuzzy50Words,
    Long fuzzy50Segments,
    Long noMatchWords,
    Long noMatchSegments
) {}
