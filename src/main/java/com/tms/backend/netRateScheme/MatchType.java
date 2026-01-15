package com.tms.backend.netRateScheme;

public enum MatchType {
    REPETITIONS("Repetitions"),
    PERCENT_101("101%"),
    PERCENT_100("100%"),
    PERCENT_95_99("95–99%"),
    PERCENT_85_94("85–94%"),
    PERCENT_75_84("75–84%"),
    PERCENT_50_74("50–74%"),
    PERCENT_0_49("0–49%");

    private final String label;

    MatchType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
