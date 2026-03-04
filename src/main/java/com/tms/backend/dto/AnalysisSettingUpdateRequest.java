package com.tms.backend.dto;

import com.tms.backend.settingAnalysis.AnalysisScope;

public record AnalysisSettingUpdateRequest(

    // include
    boolean transMemMatch,
    boolean internalFuzz,
    boolean separateInternalFuzz,
    boolean nonTranslatables,
    boolean machineTransSuggestion,

    // exclude
    boolean confirmedSegments,
    boolean lockedSegments,
    boolean numbers,

    // analyzeBy
    boolean provider,
    boolean language,

    // scope
    AnalysisScope scope
) {}
