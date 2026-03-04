package com.tms.backend.settingAnalysis;

public enum AnalysisType {
    DEFAULT("Default"),
    POST_EDITING("Post-editing");

    private final String displayValue;
    
    AnalysisType(String displayValue) {
        this.displayValue = displayValue;
    }
    
    public String getDisplayValue() {
        return displayValue;
    }
}
