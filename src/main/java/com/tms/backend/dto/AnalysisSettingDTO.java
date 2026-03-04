package com.tms.backend.dto;

import com.tms.backend.settingAnalysis.AnalysisType;

public record AnalysisSettingDTO(
    Long userId,
    AnalysisType analysisType,
    String name
) {}
