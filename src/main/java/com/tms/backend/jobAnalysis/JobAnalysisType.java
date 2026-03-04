package com.tms.backend.jobAnalysis;

public enum JobAnalysisType {
    DEFAULT("Default"),
    POST_EDITING("Post-Editing"),
    COMPARE("Compare");

    private final String name;

    JobAnalysisType(String name) {
        this.name = name;
    }

    public String getName() { return name; }
}
