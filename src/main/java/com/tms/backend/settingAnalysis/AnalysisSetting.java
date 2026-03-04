package com.tms.backend.settingAnalysis;

import com.tms.backend.user.User;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class AnalysisSetting {
    @Id
    @GeneratedValue
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Enumerated(EnumType.STRING)
    private AnalysisType analysisType = AnalysisType.DEFAULT;

    private String name = "Analysis {}";

    // include
    private boolean transMemMatch;
    private boolean internalFuzz;
    private boolean separateInternalFuzz;
    private boolean nonTranslatables;
    private boolean machineTransSuggestion; //qps

    // exclude
    private boolean confirmedSegments;
    private boolean lockedSegments;
    private boolean excludeNumbers;

    // analyzeBy
    private boolean analyzeByProvider;
    private boolean analyzeByLanguage;

    // count units of
    @Enumerated(EnumType.STRING)
    private AnalysisScope scope = AnalysisScope.SOURCE;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public AnalysisType getAnalysisType() { return analysisType; }
    public void setAnalysisType(AnalysisType analysisType) {
        this.analysisType = analysisType;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public boolean isTransMemMatch() {
        return transMemMatch;
    }
    public void setTransMemMatch(boolean transMemMatch) {
        this.transMemMatch = transMemMatch;
    }

    public boolean isInternalFuzz() {
        return internalFuzz;
    }
    public void setInternalFuzz(boolean internalFuzz) {
        this.internalFuzz = internalFuzz;
    }

    public boolean isSeparateInternalFuzz() {
        return separateInternalFuzz;
    }
    public void setSeparateInternalFuzz(boolean separateInternalFuzz) {
        this.separateInternalFuzz = separateInternalFuzz;
    }

    public boolean isNonTranslatables() {
        return nonTranslatables;
    }
    public void setNonTranslatables(boolean nonTranslatables) {
        this.nonTranslatables = nonTranslatables;
    }

    public boolean isMachineTransSuggestion() {
        return machineTransSuggestion;
    }
    public void setMachineTransSuggestion(boolean machineTransSuggestion) {
        this.machineTransSuggestion = machineTransSuggestion;
    }

    public boolean isConfirmedSegments() {
        return confirmedSegments;
    }
    public void setConfirmedSegments(boolean confirmedSegments) {
        this.confirmedSegments = confirmedSegments;
    }

    public boolean isLockedSegments() {
        return lockedSegments;
    }
    public void setLockedSegments(boolean lockedSegments) {
        this.lockedSegments = lockedSegments;
    }

    public AnalysisScope getScope() {
        return scope;
    }
    public void setScope(AnalysisScope scope) {
        this.scope = scope;
    }

    public boolean isExcludeNumbers() {
        return excludeNumbers;
    }
    public void setExcludeNumbers(boolean excludeNumbers) {
        this.excludeNumbers = excludeNumbers;
    }

    public boolean isAnalyzeByProvider() {
        return analyzeByProvider;
    }
    public void setAnalyzeByProvider(boolean analyzeByProvider) {
        this.analyzeByProvider = analyzeByProvider;
    }

    public boolean isAnalyzeByLanguage() {
        return analyzeByLanguage;
    }
    public void setAnalyzeByLanguage(boolean analyzeByLanguage) {
        this.analyzeByLanguage = analyzeByLanguage;
    }
}
