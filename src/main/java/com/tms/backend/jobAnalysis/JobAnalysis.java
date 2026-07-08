package com.tms.backend.jobAnalysis;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.tms.backend.project.Project;
import com.tms.backend.settingAnalysis.AnalysisScope;
import com.tms.backend.user.User;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;

@Entity
public class JobAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "tomato_job_id")
    private String tomatoJobId;

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

    @OneToMany(mappedBy = "jobAnalysis", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<JobAnalysisFile> files = new ArrayList<>();

    // sizing results (TM words, characters, and segments)
    private Long repetitionWords;
    private Long repetitionCharacters;
    private Long repetitionSegments;
    private Long contextMatchWords;
    private Long contextMatchCharacters;
    private Long contextMatchSegments;
    private Long perfect100Words;
    private Long perfect100Characters;
    private Long perfect100Segments;
    private Long fuzzy95Words;
    private Long fuzzy95Characters;
    private Long fuzzy95Segments;
    private Long fuzzy85Words;
    private Long fuzzy85Characters;
    private Long fuzzy85Segments;
    private Long fuzzy75Words;
    private Long fuzzy75Characters;
    private Long fuzzy75Segments;
    private Long fuzzy50Words;
    private Long fuzzy50Characters;
    private Long fuzzy50Segments;
    private Long noMatchWords;
    private Long noMatchCharacters;
    private Long noMatchSegments;

    // TM breakdown of word counts
    private Long perfect100WordsTM;
    private Long fuzzy95WordsTM;
    private Long fuzzy85WordsTM;
    private Long fuzzy75WordsTM;
    private Long fuzzy50WordsTM;
    private Long noMatchWordsTM;

    // NT breakdown of word counts
    private Long perfect100WordsNT;
    private Long fuzzy95WordsNT;
    private Long fuzzy85WordsNT;
    private Long fuzzy75WordsNT;
    private Long fuzzy50WordsNT;
    private Long noMatchWordsNT;

    // TM breakdown of character counts
    private Long perfect100CharactersTM;
    private Long fuzzy95CharactersTM;
    private Long fuzzy85CharactersTM;
    private Long fuzzy75CharactersTM;
    private Long fuzzy50CharactersTM;
    private Long noMatchCharactersTM;

    // NT breakdown of character counts
    private Long perfect100CharactersNT;
    private Long fuzzy95CharactersNT;
    private Long fuzzy85CharactersNT;
    private Long fuzzy75CharactersNT;
    private Long fuzzy50CharactersNT;
    private Long noMatchCharactersNT;

    // weighted values
    private Double approvedTM_Weighted;
    private Double approvedNT_Weighted;
    private Double repetitionTM_Weighted;
    private Double repetitionNT_Weighted;
    private Double context101TM_Weighted;
    private Double context101NT_Weighted;
    private Double perfect100TM_Weighted;
    private Double perfect100NT_Weighted;
    private Double fuzzy95TM_Weighted;
    private Double fuzzy95NT_Weighted;
    private Double fuzzy85TM_Weighted;
    private Double fuzzy85NT_Weighted;
    private Double fuzzy75TM_Weighted;
    private Double fuzzy75NT_Weighted;
    private Double fuzzy50TM_Weighted;
    private Double fuzzy50NT_Weighted;
    private Double noMatchTM_Weighted;
    private Double noMatchNT_Weighted;
    private Double totalWeighted;
    private Double totalWeightedPercentage;
    private Long allWords;
    private Long allCharacters;
    private String unitType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_analysis_tm_names",
                    joinColumns = @JoinColumn(name = "job_analysis_id"))
    private List<String> tmNames;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTomatoJobId() { return tomatoJobId; }
    public void setTomatoJobId(String tomatoJobId) { this.tomatoJobId = tomatoJobId; }

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

    public Long getRepetitionCharacters() { return repetitionCharacters; }
    public void setRepetitionCharacters(Long repetitionCharacters) { this.repetitionCharacters = repetitionCharacters; }

    public Long getRepetitionSegments() { return repetitionSegments; }
    public void setRepetitionSegments(Long repetitionSegments) { this.repetitionSegments = repetitionSegments; }

    public Long getContextMatchWords() { return contextMatchWords; }
    public void setContextMatchWords(Long contextMatchWords) { this.contextMatchWords = contextMatchWords; }

    public Long getContextMatchCharacters() { return contextMatchCharacters; }
    public void setContextMatchCharacters(Long contextMatchCharacters) { this.contextMatchCharacters = contextMatchCharacters; }

    public Long getContextMatchSegments() { return contextMatchSegments; }
    public void setContextMatchSegments(Long contextMatchSegments) { this.contextMatchSegments = contextMatchSegments; }

    public Long getPerfect100Words() { return perfect100Words; }
    public void setPerfect100Words(Long perfect100Words) { this.perfect100Words = perfect100Words; }

    public Long getPerfect100Characters() { return perfect100Characters; }
    public void setPerfect100Characters(Long perfect100Characters) { this.perfect100Characters = perfect100Characters; }

    public Long getPerfect100Segments() { return perfect100Segments; }
    public void setPerfect100Segments(Long perfect100Segments) { this.perfect100Segments = perfect100Segments; }

    public Long getFuzzy95Words() { return fuzzy95Words; }
    public void setFuzzy95Words(Long fuzzy95Words) { this.fuzzy95Words = fuzzy95Words; }

    public Long getFuzzy95Characters() { return fuzzy95Characters; }
    public void setFuzzy95Characters(Long fuzzy95Characters) { this.fuzzy95Characters = fuzzy95Characters; }

    public Long getFuzzy95Segments() { return fuzzy95Segments; }
    public void setFuzzy95Segments(Long fuzzy95Segments) { this.fuzzy95Segments = fuzzy95Segments; }

    public Long getFuzzy85Words() { return fuzzy85Words; }
    public void setFuzzy85Words(Long fuzzy85Words) { this.fuzzy85Words = fuzzy85Words; }

    public Long getFuzzy85Characters() { return fuzzy85Characters; }
    public void setFuzzy85Characters(Long fuzzy85Characters) { this.fuzzy85Characters = fuzzy85Characters; }

    public Long getFuzzy85Segments() { return fuzzy85Segments; }
    public void setFuzzy85Segments(Long fuzzy85Segments) { this.fuzzy85Segments = fuzzy85Segments; }

    public Long getFuzzy75Words() { return fuzzy75Words; }
    public void setFuzzy75Words(Long fuzzy75Words) { this.fuzzy75Words = fuzzy75Words; }

    public Long getFuzzy75Characters() { return fuzzy75Characters; }
    public void setFuzzy75Characters(Long fuzzy75Characters) { this.fuzzy75Characters = fuzzy75Characters; }

    public Long getFuzzy75Segments() { return fuzzy75Segments; }
    public void setFuzzy75Segments(Long fuzzy75Segments) { this.fuzzy75Segments = fuzzy75Segments; }

    public Long getFuzzy50Words() { return fuzzy50Words; }
    public void setFuzzy50Words(Long fuzzy50Words) { this.fuzzy50Words = fuzzy50Words; }

    public Long getFuzzy50Characters() { return fuzzy50Characters; }
    public void setFuzzy50Characters(Long fuzzy50Characters) { this.fuzzy50Characters = fuzzy50Characters; }

    public Long getFuzzy50Segments() { return fuzzy50Segments; }
    public void setFuzzy50Segments(Long fuzzy50Segments) { this.fuzzy50Segments = fuzzy50Segments; }

    public Long getNoMatchWords() { return noMatchWords; }
    public void setNoMatchWords(Long noMatchWords) { this.noMatchWords = noMatchWords; }

    public Long getNoMatchCharacters() { return noMatchCharacters; }
    public void setNoMatchCharacters(Long noMatchCharacters) { this.noMatchCharacters = noMatchCharacters; }

    public Long getNoMatchSegments() { return noMatchSegments; }
    public void setNoMatchSegments(Long noMatchSegments) { this.noMatchSegments = noMatchSegments; }

    public Long getPerfect100WordsTM() { return perfect100WordsTM; }
    public void setPerfect100WordsTM(Long perfect100WordsTM) { this.perfect100WordsTM = perfect100WordsTM; }

    public Long getPerfect100WordsNT() { return perfect100WordsNT; }
    public void setPerfect100WordsNT(Long perfect100WordsNT) { this.perfect100WordsNT = perfect100WordsNT; }

    public Long getFuzzy95WordsTM() { return fuzzy95WordsTM; }
    public void setFuzzy95WordsTM(Long fuzzy95WordsTM) { this.fuzzy95WordsTM = fuzzy95WordsTM; }

    public Long getFuzzy95WordsNT() { return fuzzy95WordsNT; }
    public void setFuzzy95WordsNT(Long fuzzy95WordsNT) { this.fuzzy95WordsNT = fuzzy95WordsNT; }

    public Long getFuzzy85WordsTM() { return fuzzy85WordsTM; }
    public void setFuzzy85WordsTM(Long fuzzy85WordsTM) { this.fuzzy85WordsTM = fuzzy85WordsTM; }

    public Long getFuzzy85WordsNT() { return fuzzy85WordsNT; }
    public void setFuzzy85WordsNT(Long fuzzy85WordsNT) { this.fuzzy85WordsNT = fuzzy85WordsNT; }

    public Long getFuzzy75WordsTM() { return fuzzy75WordsTM; }
    public void setFuzzy75WordsTM(Long fuzzy75WordsTM) { this.fuzzy75WordsTM = fuzzy75WordsTM; }

    public Long getFuzzy75WordsNT() { return fuzzy75WordsNT; }
    public void setFuzzy75WordsNT(Long fuzzy75WordsNT) { this.fuzzy75WordsNT = fuzzy75WordsNT; }

    public Long getFuzzy50WordsTM() { return fuzzy50WordsTM; }
    public void setFuzzy50WordsTM(Long fuzzy50WordsTM) { this.fuzzy50WordsTM = fuzzy50WordsTM; }

    public Long getFuzzy50WordsNT() { return fuzzy50WordsNT; }
    public void setFuzzy50WordsNT(Long fuzzy50WordsNT) { this.fuzzy50WordsNT = fuzzy50WordsNT; }

    public Long getNoMatchWordsTM() { return noMatchWordsTM; }
    public void setNoMatchWordsTM(Long noMatchWordsTM) { this.noMatchWordsTM = noMatchWordsTM; }

    public Long getNoMatchWordsNT() { return noMatchWordsNT; }
    public void setNoMatchWordsNT(Long noMatchWordsNT) { this.noMatchWordsNT = noMatchWordsNT; }

    public Long getPerfect100CharactersTM() { return perfect100CharactersTM; }
    public void setPerfect100CharactersTM(Long perfect100CharactersTM) { this.perfect100CharactersTM = perfect100CharactersTM; }

    public Long getFuzzy95CharactersTM() { return fuzzy95CharactersTM; }
    public void setFuzzy95CharactersTM(Long fuzzy95CharactersTM) { this.fuzzy95CharactersTM = fuzzy95CharactersTM; }

    public Long getFuzzy85CharactersTM() { return fuzzy85CharactersTM; }
    public void setFuzzy85CharactersTM(Long fuzzy85CharactersTM) { this.fuzzy85CharactersTM = fuzzy85CharactersTM; }

    public Long getFuzzy75CharactersTM() { return fuzzy75CharactersTM; }
    public void setFuzzy75CharactersTM(Long fuzzy75CharactersTM) { this.fuzzy75CharactersTM = fuzzy75CharactersTM; }

    public Long getFuzzy50CharactersTM() { return fuzzy50CharactersTM; }
    public void setFuzzy50CharactersTM(Long fuzzy50CharactersTM) { this.fuzzy50CharactersTM = fuzzy50CharactersTM; }

    public Long getNoMatchCharactersTM() { return noMatchCharactersTM; }
    public void setNoMatchCharactersTM(Long noMatchCharactersTM) { this.noMatchCharactersTM = noMatchCharactersTM; }

    public Long getPerfect100CharactersNT() { return perfect100CharactersNT; }
    public void setPerfect100CharactersNT(Long perfect100CharactersNT) { this.perfect100CharactersNT = perfect100CharactersNT; }

    public Long getFuzzy95CharactersNT() { return fuzzy95CharactersNT; }
    public void setFuzzy95CharactersNT(Long fuzzy95CharactersNT) { this.fuzzy95CharactersNT = fuzzy95CharactersNT; }

    public Long getFuzzy85CharactersNT() { return fuzzy85CharactersNT; }
    public void setFuzzy85CharactersNT(Long fuzzy85CharactersNT) { this.fuzzy85CharactersNT = fuzzy85CharactersNT; }

    public Long getFuzzy75CharactersNT() { return fuzzy75CharactersNT; }
    public void setFuzzy75CharactersNT(Long fuzzy75CharactersNT) { this.fuzzy75CharactersNT = fuzzy75CharactersNT; }

    public Long getFuzzy50CharactersNT() { return fuzzy50CharactersNT; }
    public void setFuzzy50CharactersNT(Long fuzzy50CharactersNT) { this.fuzzy50CharactersNT = fuzzy50CharactersNT; }

    public Long getNoMatchCharactersNT() { return noMatchCharactersNT; }
    public void setNoMatchCharactersNT(Long noMatchCharactersNT) { this.noMatchCharactersNT = noMatchCharactersNT; }

    public List<JobAnalysisFile> getFiles() { return files; }
    public void setFiles(List<JobAnalysisFile> files) { this.files = files; }

    public Double getApprovedTM_Weighted() { return approvedTM_Weighted; }
    public void setApprovedTM_Weighted(Double v) { this.approvedTM_Weighted = v; }

    public Double getApprovedNT_Weighted() { return approvedNT_Weighted; }
    public void setApprovedNT_Weighted(Double v) { this.approvedNT_Weighted = v; }

    public Double getRepetitionTM_Weighted() { return repetitionTM_Weighted; }
    public void setRepetitionTM_Weighted(Double v) { this.repetitionTM_Weighted = v; }

    public Double getRepetitionNT_Weighted() { return repetitionNT_Weighted; }
    public void setRepetitionNT_Weighted(Double v) { this.repetitionNT_Weighted = v; }

    public Double getContext101TM_Weighted() { return context101TM_Weighted; }
    public void setContext101TM_Weighted(Double v) { this.context101TM_Weighted = v; }

    public Double getContext101NT_Weighted() { return context101NT_Weighted; }
    public void setContext101NT_Weighted(Double v) { this.context101NT_Weighted = v; }

    public Double getPerfect100TM_Weighted() { return perfect100TM_Weighted; }
    public void setPerfect100TM_Weighted(Double v) { this.perfect100TM_Weighted = v; }

    public Double getPerfect100NT_Weighted() { return perfect100NT_Weighted; }
    public void setPerfect100NT_Weighted(Double v) { this.perfect100NT_Weighted = v; }

    public Double getFuzzy95TM_Weighted() { return fuzzy95TM_Weighted; }
    public void setFuzzy95TM_Weighted(Double v) { this.fuzzy95TM_Weighted = v; }

    public Double getFuzzy95NT_Weighted() { return fuzzy95NT_Weighted; }
    public void setFuzzy95NT_Weighted(Double v) { this.fuzzy95NT_Weighted = v; }

    public Double getFuzzy85TM_Weighted() { return fuzzy85TM_Weighted; }
    public void setFuzzy85TM_Weighted(Double v) { this.fuzzy85TM_Weighted = v; }

    public Double getFuzzy85NT_Weighted() { return fuzzy85NT_Weighted; }
    public void setFuzzy85NT_Weighted(Double v) { this.fuzzy85NT_Weighted = v; }

    public Double getFuzzy75TM_Weighted() { return fuzzy75TM_Weighted; }
    public void setFuzzy75TM_Weighted(Double v) { this.fuzzy75TM_Weighted = v; }

    public Double getFuzzy75NT_Weighted() { return fuzzy75NT_Weighted; }
    public void setFuzzy75NT_Weighted(Double v) { this.fuzzy75NT_Weighted = v; }

    public Double getFuzzy50TM_Weighted() { return fuzzy50TM_Weighted; }
    public void setFuzzy50TM_Weighted(Double v) { this.fuzzy50TM_Weighted = v; }

    public Double getFuzzy50NT_Weighted() { return fuzzy50NT_Weighted; }
    public void setFuzzy50NT_Weighted(Double v) { this.fuzzy50NT_Weighted = v; }

    public Double getNoMatchTM_Weighted() { return noMatchTM_Weighted; }
    public void setNoMatchTM_Weighted(Double v) { this.noMatchTM_Weighted = v; }

    public Double getNoMatchNT_Weighted() { return noMatchNT_Weighted; }
    public void setNoMatchNT_Weighted(Double v) { this.noMatchNT_Weighted = v; }

    public Double getTotalWeighted() { return totalWeighted; }
    public void setTotalWeighted(Double v) { this.totalWeighted = v; }

    public Double getTotalWeightedPercentage() { return totalWeightedPercentage; }
    public void setTotalWeightedPercentage(Double v) { this.totalWeightedPercentage = v; }

    public Long getAllWords() { return allWords; }
    public void setAllWords(Long v) { this.allWords = v; }

    public Long getAllCharacters() { return allCharacters; }
    public void setAllCharacters(Long v) { this.allCharacters = v; }

    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType; }

    public List<String> getTmNames() { return tmNames; }
    public void setTmNames(List<String> tmNames) { this.tmNames = tmNames; }
}