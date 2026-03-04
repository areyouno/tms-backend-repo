package com.tms.backend.jobAnalysis;


import java.time.LocalDateTime;
import java.util.Set;

import com.tms.backend.settingAnalysis.AnalysisScope;
import com.tms.backend.user.User;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class JobAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private JobAnalysisType type = JobAnalysisType.DEFAULT;

    private LocalDateTime createDate;
    private String createdBy;

    private String sourceLang;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_analysis_languages", 
                    joinColumns = @JoinColumn(name = "job_analysis_id"))
    private Set<String> targetLanguages;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private User provider;

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
    private boolean analyzeBylanguage;

    // count units of
    @Enumerated(EnumType.STRING)
    private AnalysisScope scope = AnalysisScope.SOURCE;

    // net rate results
    private Double netRateRepetition;
    private Double netRateContextMatch;
    private Double netRatePerfect100;
    private Double netRateFuzzy95;
    private Double netRateFuzzy85;
    private Double netRateFuzzy75;
    private Double netRateFuzzy50;
    private Double netRateNoMatch;
    private Double netRateTotal;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public JobAnalysisType getType() { return type; }
    public void setType(JobAnalysisType type) { this.type = type; }

    public User getProvider() { return provider; }
    public void setProvider(User provider) { this.provider = provider; }

    public LocalDateTime getCreateDate() { return createDate; }
    public void setCreateDate(LocalDateTime createDate) { this.createDate = createDate; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getSourceLang() { return sourceLang; }
    public void setSourceLang(String sourceLang) { this.sourceLang = sourceLang; }

    public Set<String> getTargetLanguages() { return targetLanguages; }
    public void setTargetLanguages(Set<String> targetLanguages) { this.targetLanguages = targetLanguages; }

    public boolean isTransMemMatch() { return transMemMatch; }
    public void setTransMemMatch(boolean transMemMatch) { this.transMemMatch = transMemMatch; }

    public boolean isInternalFuzz() { return internalFuzz; }
    public void setInternalFuzz(boolean internalFuzz) { this.internalFuzz = internalFuzz; }

    public boolean isSeparateInternalFuzz() { return separateInternalFuzz; }
    public void setSeparateInternalFuzz(boolean separateInternalFuzz) { this.separateInternalFuzz = separateInternalFuzz; }

    public boolean isNonTranslatables() { return nonTranslatables; }
    public void setNonTranslatables(boolean nonTranslatables) { this.nonTranslatables = nonTranslatables; }

    public boolean isMachineTransSuggestion() { return machineTransSuggestion; }
    public void setMachineTransSuggestion(boolean machineTransSuggestion) { this.machineTransSuggestion = machineTransSuggestion; }
    
    public boolean isConfirmedSegments() { return confirmedSegments; }
    public void setConfirmedSegments(boolean confirmedSegments) { this.confirmedSegments = confirmedSegments; }

    public boolean isLockedSegments() { return lockedSegments; }
    public void setLockedSegments(boolean lockedSegments) { this.lockedSegments = lockedSegments; }

    public boolean isExcludeNumbers() { return excludeNumbers; }
    public void setExcludeNumbers(boolean excludeNumbers) { this.excludeNumbers = excludeNumbers; }

    public boolean isAnalyzeByProvider() { return analyzeByProvider; }
    public void setAnalyzeByProvider(boolean analyzeByProvider) { this.analyzeByProvider = analyzeByProvider; }

    public boolean isAnalyzeBylanguage() { return analyzeBylanguage; }
    public void setAnalyzeBylanguage(boolean analyzeBylanguage) { this.analyzeBylanguage = analyzeBylanguage; }

    public AnalysisScope getScope() { return scope; }
    public void setScope(AnalysisScope scope) { this.scope = scope; }

    public Double getNetRateRepetition() { return netRateRepetition; }
    public void setNetRateRepetition(Double netRateRepetition) { this.netRateRepetition = netRateRepetition; }

    public Double getNetRateContextMatch() { return netRateContextMatch; }
    public void setNetRateContextMatch(Double netRateContextMatch) { this.netRateContextMatch = netRateContextMatch; }

    public Double getNetRatePerfect100() { return netRatePerfect100; }
    public void setNetRatePerfect100(Double netRatePerfect100) { this.netRatePerfect100 = netRatePerfect100; }

    public Double getNetRateFuzzy95() { return netRateFuzzy95; }
    public void setNetRateFuzzy95(Double netRateFuzzy95) { this.netRateFuzzy95 = netRateFuzzy95; }

    public Double getNetRateFuzzy85() { return netRateFuzzy85; }
    public void setNetRateFuzzy85(Double netRateFuzzy85) { this.netRateFuzzy85 = netRateFuzzy85; }

    public Double getNetRateFuzzy75() { return netRateFuzzy75; }
    public void setNetRateFuzzy75(Double netRateFuzzy75) { this.netRateFuzzy75 = netRateFuzzy75; }

    public Double getNetRateFuzzy50() { return netRateFuzzy50; }
    public void setNetRateFuzzy50(Double netRateFuzzy50) { this.netRateFuzzy50 = netRateFuzzy50; }

    public Double getNetRateNoMatch() { return netRateNoMatch; }
    public void setNetRateNoMatch(Double netRateNoMatch) { this.netRateNoMatch = netRateNoMatch; }

    public Double getNetRateTotal() { return netRateTotal; }
    public void setNetRateTotal(Double netRateTotal) { this.netRateTotal = netRateTotal; }
}
