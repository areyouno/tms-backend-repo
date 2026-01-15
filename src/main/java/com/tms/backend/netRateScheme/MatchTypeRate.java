package com.tms.backend.netRateScheme;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class MatchTypeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    private MatchType matchType;

    private Long transMemoryPercent;
    private Long machineTransPercent;
    private Long nonTranslatablePercent;
    private Long internalFuzziesPercent;

    public MatchTypeRate() {}

    public MatchTypeRate(MatchType matchType, Long transMemoryPercent, Long machineTransPercent,
                         Long nonTranslatablePercent, Long internalFuzziesPercent) {
        this.matchType = matchType;
        this.transMemoryPercent = transMemoryPercent;
        this.machineTransPercent = machineTransPercent;
        this.nonTranslatablePercent = nonTranslatablePercent;
        this.internalFuzziesPercent = internalFuzziesPercent;
    }

    // Getters and setters
    public MatchType getMatchType() { return matchType; }
    public void setMatchType(MatchType matchType) { this.matchType = matchType; }

    public Long getTransMemoryPercent() { return transMemoryPercent; }
    public void setTransMemoryPercent(Long transMemoryPercent) { this.transMemoryPercent = transMemoryPercent; }

    public Long getMachineTransPercent() { return machineTransPercent; }
    public void setMachineTransPercent(Long machineTransPercent) { this.machineTransPercent = machineTransPercent; }

    public Long getNonTranslatablePercent() { return nonTranslatablePercent; }
    public void setNonTranslatablePercent(Long nonTranslatablePercent) { this.nonTranslatablePercent = nonTranslatablePercent; }

    public Long getInternalFuzziesPercent() { return internalFuzziesPercent; }
    public void setInternalFuzziesPercent(Long internalFuzziesPercent) { this.internalFuzziesPercent = internalFuzziesPercent; }
}
