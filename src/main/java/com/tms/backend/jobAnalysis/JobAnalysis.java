package com.tms.backend.jobAnalysis;


import java.time.LocalDateTime;
import java.util.Set;

import com.tms.backend.project.Project;
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
    @JoinColumn(name = "project_id")
    private Project project;

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

    // sizing results (TM words and segments)
    private Long repetitionWords;
    private Long repetitionSegments;
    private Long contextMatchWords;
    private Long contextMatchSegments;
    private Long perfect100Words;
    private Long perfect100Segments;
    private Long fuzzy95Words;
    private Long fuzzy95Segments;
    private Long fuzzy85Words;
    private Long fuzzy85Segments;
    private Long fuzzy75Words;
    private Long fuzzy75Segments;
    private Long fuzzy50Words;
    private Long fuzzy50Segments;
    private Long noMatchWords;
    private Long noMatchSegments;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

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

    public Long getRepetitionWords() { return repetitionWords; }
    public void setRepetitionWords(Long repetitionWords) { this.repetitionWords = repetitionWords; }

    public Long getRepetitionSegments() { return repetitionSegments; }
    public void setRepetitionSegments(Long repetitionSegments) { this.repetitionSegments = repetitionSegments; }

    public Long getContextMatchWords() { return contextMatchWords; }
    public void setContextMatchWords(Long contextMatchWords) { this.contextMatchWords = contextMatchWords; }

    public Long getContextMatchSegments() { return contextMatchSegments; }
    public void setContextMatchSegments(Long contextMatchSegments) { this.contextMatchSegments = contextMatchSegments; }

    public Long getPerfect100Words() { return perfect100Words; }
    public void setPerfect100Words(Long perfect100Words) { this.perfect100Words = perfect100Words; }

    public Long getPerfect100Segments() { return perfect100Segments; }
    public void setPerfect100Segments(Long perfect100Segments) { this.perfect100Segments = perfect100Segments; }

    public Long getFuzzy95Words() { return fuzzy95Words; }
    public void setFuzzy95Words(Long fuzzy95Words) { this.fuzzy95Words = fuzzy95Words; }

    public Long getFuzzy95Segments() { return fuzzy95Segments; }
    public void setFuzzy95Segments(Long fuzzy95Segments) { this.fuzzy95Segments = fuzzy95Segments; }

    public Long getFuzzy85Words() { return fuzzy85Words; }
    public void setFuzzy85Words(Long fuzzy85Words) { this.fuzzy85Words = fuzzy85Words; }

    public Long getFuzzy85Segments() { return fuzzy85Segments; }
    public void setFuzzy85Segments(Long fuzzy85Segments) { this.fuzzy85Segments = fuzzy85Segments; }

    public Long getFuzzy75Words() { return fuzzy75Words; }
    public void setFuzzy75Words(Long fuzzy75Words) { this.fuzzy75Words = fuzzy75Words; }

    public Long getFuzzy75Segments() { return fuzzy75Segments; }
    public void setFuzzy75Segments(Long fuzzy75Segments) { this.fuzzy75Segments = fuzzy75Segments; }

    public Long getFuzzy50Words() { return fuzzy50Words; }
    public void setFuzzy50Words(Long fuzzy50Words) { this.fuzzy50Words = fuzzy50Words; }

    public Long getFuzzy50Segments() { return fuzzy50Segments; }
    public void setFuzzy50Segments(Long fuzzy50Segments) { this.fuzzy50Segments = fuzzy50Segments; }

    public Long getNoMatchWords() { return noMatchWords; }
    public void setNoMatchWords(Long noMatchWords) { this.noMatchWords = noMatchWords; }

    public Long getNoMatchSegments() { return noMatchSegments; }
    public void setNoMatchSegments(Long noMatchSegments) { this.noMatchSegments = noMatchSegments; }
}
