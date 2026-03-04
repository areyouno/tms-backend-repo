package com.tms.backend.netRateScheme;

public enum MatchType {
    REPETITIONS("Repetitions"),
    PERCENT_101("101%"),
    PERCENT_100("100%"),
    PERCENT_95("95–99%"),
    PERCENT_85("85–94%"),
    PERCENT_75("75–84%"),
    PERCENT_50("50–74%"),
    PERCENT_0("0–49%");

    private final String label;

    MatchType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
