package com.tms.backend.jobAnalysis;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class JobAnalysisFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_analysis_id")
    private JobAnalysis jobAnalysis;

    private String fileName;

    private Long approvedSegments;
    private Long approvedWords;
    private Long approvedCharacters;
    private Double approvedWeighted;
    private Double approvedPercentage;

    private Long repetitionSegments;
    private Long repetitionWords;
    private Long repetitionCharacters;
    private Double repetitionWeighted;
    private Double repetitionPercentage;

    private Long context101Segments;
    private Long context101Words;
    private Long context101Characters;
    private Double context101Weighted;
    private Double context101Percentage;

    private Long perfect100Segments;
    private Long perfect100Words;
    private Long perfect100Characters;
    private Double perfect100Weighted;
    private Double perfect100Percentage;

    private Long fuzzy95Segments;
    private Long fuzzy95Words;
    private Long fuzzy95Characters;
    private Double fuzzy95Weighted;
    private Double fuzzy95Percentage;

    private Long fuzzy85Segments;
    private Long fuzzy85Words;
    private Long fuzzy85Characters;
    private Double fuzzy85Weighted;
    private Double fuzzy85Percentage;

    private Long fuzzy75Segments;
    private Long fuzzy75Words;
    private Long fuzzy75Characters;
    private Double fuzzy75Weighted;
    private Double fuzzy75Percentage;

    private Long fuzzy50Segments;
    private Long fuzzy50Words;
    private Long fuzzy50Characters;
    private Double fuzzy50Weighted;
    private Double fuzzy50Percentage;

    private Long noMatchSegments;
    private Long noMatchWords;
    private Long noMatchCharacters;
    private Double noMatchWeighted;
    private Double noMatchPercentage;

    private Long totalSegments;
    private Long totalWords;
    private Long totalCharacters;
    private Double totalWeighted;
    private Double totalWeightedPercentage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public JobAnalysis getJobAnalysis() { return jobAnalysis; }
    public void setJobAnalysis(JobAnalysis jobAnalysis) { this.jobAnalysis = jobAnalysis; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getApprovedSegments() { return approvedSegments; }
    public void setApprovedSegments(Long v) { this.approvedSegments = v; }

    public Long getApprovedWords() { return approvedWords; }
    public void setApprovedWords(Long v) { this.approvedWords = v; }

    public Long getApprovedCharacters() { return approvedCharacters; }
    public void setApprovedCharacters(Long v) { this.approvedCharacters = v; }

    public Double getApprovedWeighted() { return approvedWeighted; }
    public void setApprovedWeighted(Double v) { this.approvedWeighted = v; }

    public Double getApprovedPercentage() { return approvedPercentage; }
    public void setApprovedPercentage(Double v) { this.approvedPercentage = v; }

    public Long getRepetitionSegments() { return repetitionSegments; }
    public void setRepetitionSegments(Long v) { this.repetitionSegments = v; }

    public Long getRepetitionWords() { return repetitionWords; }
    public void setRepetitionWords(Long v) { this.repetitionWords = v; }

    public Long getRepetitionCharacters() { return repetitionCharacters; }
    public void setRepetitionCharacters(Long v) { this.repetitionCharacters = v; }

    public Double getRepetitionWeighted() { return repetitionWeighted; }
    public void setRepetitionWeighted(Double v) { this.repetitionWeighted = v; }

    public Double getRepetitionPercentage() { return repetitionPercentage; }
    public void setRepetitionPercentage(Double v) { this.repetitionPercentage = v; }

    public Long getContext101Segments() { return context101Segments; }
    public void setContext101Segments(Long v) { this.context101Segments = v; }

    public Long getContext101Words() { return context101Words; }
    public void setContext101Words(Long v) { this.context101Words = v; }

    public Long getContext101Characters() { return context101Characters; }
    public void setContext101Characters(Long v) { this.context101Characters = v; }

    public Double getContext101Weighted() { return context101Weighted; }
    public void setContext101Weighted(Double v) { this.context101Weighted = v; }

    public Double getContext101Percentage() { return context101Percentage; }
    public void setContext101Percentage(Double v) { this.context101Percentage = v; }

    public Long getPerfect100Segments() { return perfect100Segments; }
    public void setPerfect100Segments(Long v) { this.perfect100Segments = v; }

    public Long getPerfect100Words() { return perfect100Words; }
    public void setPerfect100Words(Long v) { this.perfect100Words = v; }

    public Long getPerfect100Characters() { return perfect100Characters; }
    public void setPerfect100Characters(Long v) { this.perfect100Characters = v; }

    public Double getPerfect100Weighted() { return perfect100Weighted; }
    public void setPerfect100Weighted(Double v) { this.perfect100Weighted = v; }

    public Double getPerfect100Percentage() { return perfect100Percentage; }
    public void setPerfect100Percentage(Double v) { this.perfect100Percentage = v; }

    public Long getFuzzy95Segments() { return fuzzy95Segments; }
    public void setFuzzy95Segments(Long v) { this.fuzzy95Segments = v; }

    public Long getFuzzy95Words() { return fuzzy95Words; }
    public void setFuzzy95Words(Long v) { this.fuzzy95Words = v; }

    public Long getFuzzy95Characters() { return fuzzy95Characters; }
    public void setFuzzy95Characters(Long v) { this.fuzzy95Characters = v; }

    public Double getFuzzy95Weighted() { return fuzzy95Weighted; }
    public void setFuzzy95Weighted(Double v) { this.fuzzy95Weighted = v; }

    public Double getFuzzy95Percentage() { return fuzzy95Percentage; }
    public void setFuzzy95Percentage(Double v) { this.fuzzy95Percentage = v; }

    public Long getFuzzy85Segments() { return fuzzy85Segments; }
    public void setFuzzy85Segments(Long v) { this.fuzzy85Segments = v; }

    public Long getFuzzy85Words() { return fuzzy85Words; }
    public void setFuzzy85Words(Long v) { this.fuzzy85Words = v; }

    public Long getFuzzy85Characters() { return fuzzy85Characters; }
    public void setFuzzy85Characters(Long v) { this.fuzzy85Characters = v; }

    public Double getFuzzy85Weighted() { return fuzzy85Weighted; }
    public void setFuzzy85Weighted(Double v) { this.fuzzy85Weighted = v; }

    public Double getFuzzy85Percentage() { return fuzzy85Percentage; }
    public void setFuzzy85Percentage(Double v) { this.fuzzy85Percentage = v; }

    public Long getFuzzy75Segments() { return fuzzy75Segments; }
    public void setFuzzy75Segments(Long v) { this.fuzzy75Segments = v; }

    public Long getFuzzy75Words() { return fuzzy75Words; }
    public void setFuzzy75Words(Long v) { this.fuzzy75Words = v; }

    public Long getFuzzy75Characters() { return fuzzy75Characters; }
    public void setFuzzy75Characters(Long v) { this.fuzzy75Characters = v; }

    public Double getFuzzy75Weighted() { return fuzzy75Weighted; }
    public void setFuzzy75Weighted(Double v) { this.fuzzy75Weighted = v; }

    public Double getFuzzy75Percentage() { return fuzzy75Percentage; }
    public void setFuzzy75Percentage(Double v) { this.fuzzy75Percentage = v; }

    public Long getFuzzy50Segments() { return fuzzy50Segments; }
    public void setFuzzy50Segments(Long v) { this.fuzzy50Segments = v; }

    public Long getFuzzy50Words() { return fuzzy50Words; }
    public void setFuzzy50Words(Long v) { this.fuzzy50Words = v; }

    public Long getFuzzy50Characters() { return fuzzy50Characters; }
    public void setFuzzy50Characters(Long v) { this.fuzzy50Characters = v; }

    public Double getFuzzy50Weighted() { return fuzzy50Weighted; }
    public void setFuzzy50Weighted(Double v) { this.fuzzy50Weighted = v; }

    public Double getFuzzy50Percentage() { return fuzzy50Percentage; }
    public void setFuzzy50Percentage(Double v) { this.fuzzy50Percentage = v; }

    public Long getNoMatchSegments() { return noMatchSegments; }
    public void setNoMatchSegments(Long v) { this.noMatchSegments = v; }

    public Long getNoMatchWords() { return noMatchWords; }
    public void setNoMatchWords(Long v) { this.noMatchWords = v; }

    public Long getNoMatchCharacters() { return noMatchCharacters; }
    public void setNoMatchCharacters(Long v) { this.noMatchCharacters = v; }

    public Double getNoMatchWeighted() { return noMatchWeighted; }
    public void setNoMatchWeighted(Double v) { this.noMatchWeighted = v; }

    public Double getNoMatchPercentage() { return noMatchPercentage; }
    public void setNoMatchPercentage(Double v) { this.noMatchPercentage = v; }

    public Long getTotalSegments() { return totalSegments; }
    public void setTotalSegments(Long v) { this.totalSegments = v; }

    public Long getTotalWords() { return totalWords; }
    public void setTotalWords(Long v) { this.totalWords = v; }

    public Long getTotalCharacters() { return totalCharacters; }
    public void setTotalCharacters(Long v) { this.totalCharacters = v; }

    public Double getTotalWeighted() { return totalWeighted; }
    public void setTotalWeighted(Double v) { this.totalWeighted = v; }

    public Double getTotalWeightedPercentage() { return totalWeightedPercentage; }
    public void setTotalWeightedPercentage(Double v) { this.totalWeightedPercentage = v; }
}
