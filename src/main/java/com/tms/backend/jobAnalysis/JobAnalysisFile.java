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

    private Long approvedTM_Words;
    private Long approvedTM_Characters;
    private Long approvedTM_Segments;
    private Long repetitionTM_Words;
    private Long repetitionTM_Characters;
    private Long repetitionTM_Segments;
    private Long context101TM_Words;
    private Long context101TM_Characters;
    private Long context101TM_Segments;
    private Long perfect100TM_Words;
    private Long perfect100TM_Characters;
    private Long perfect100TM_Segments;
    private Long fuzzy95TM_Words;
    private Long fuzzy95TM_Characters;
    private Long fuzzy95TM_Segments;
    private Long fuzzy85TM_Words;
    private Long fuzzy85TM_Characters;
    private Long fuzzy85TM_Segments;
    private Long fuzzy75TM_Words;
    private Long fuzzy75TM_Characters;
    private Long fuzzy75TM_Segments;
    private Long fuzzy50TM_Words;
    private Long fuzzy50TM_Characters;
    private Long fuzzy50TM_Segments;
    private Long noMatchTM_Words;
    private Long noMatchTM_Characters;
    private Long noMatchTM_Segments;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public JobAnalysis getJobAnalysis() { return jobAnalysis; }
    public void setJobAnalysis(JobAnalysis jobAnalysis) { this.jobAnalysis = jobAnalysis; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getApprovedTM_Words() { return approvedTM_Words; }
    public void setApprovedTM_Words(Long approvedTM_Words) { this.approvedTM_Words = approvedTM_Words; }

    public Long getApprovedTM_Characters() { return approvedTM_Characters; }
    public void setApprovedTM_Characters(Long approvedTM_Characters) { this.approvedTM_Characters = approvedTM_Characters; }

    public Long getApprovedTM_Segments() { return approvedTM_Segments; }
    public void setApprovedTM_Segments(Long approvedTM_Segments) { this.approvedTM_Segments = approvedTM_Segments; }

    public Long getRepetitionTM_Words() { return repetitionTM_Words; }
    public void setRepetitionTM_Words(Long repetitionTM_Words) { this.repetitionTM_Words = repetitionTM_Words; }

    public Long getRepetitionTM_Characters() { return repetitionTM_Characters; }
    public void setRepetitionTM_Characters(Long repetitionTM_Characters) { this.repetitionTM_Characters = repetitionTM_Characters; }

    public Long getRepetitionTM_Segments() { return repetitionTM_Segments; }
    public void setRepetitionTM_Segments(Long repetitionTM_Segments) { this.repetitionTM_Segments = repetitionTM_Segments; }

    public Long getContext101TM_Words() { return context101TM_Words; }
    public void setContext101TM_Words(Long context101TM_Words) { this.context101TM_Words = context101TM_Words; }

    public Long getContext101TM_Characters() { return context101TM_Characters; }
    public void setContext101TM_Characters(Long context101TM_Characters) { this.context101TM_Characters = context101TM_Characters; }

    public Long getContext101TM_Segments() { return context101TM_Segments; }
    public void setContext101TM_Segments(Long context101TM_Segments) { this.context101TM_Segments = context101TM_Segments; }

    public Long getPerfect100TM_Words() { return perfect100TM_Words; }
    public void setPerfect100TM_Words(Long perfect100TM_Words) { this.perfect100TM_Words = perfect100TM_Words; }

    public Long getPerfect100TM_Characters() { return perfect100TM_Characters; }
    public void setPerfect100TM_Characters(Long perfect100TM_Characters) { this.perfect100TM_Characters = perfect100TM_Characters; }

    public Long getPerfect100TM_Segments() { return perfect100TM_Segments; }
    public void setPerfect100TM_Segments(Long perfect100TM_Segments) { this.perfect100TM_Segments = perfect100TM_Segments; }

    public Long getFuzzy95TM_Words() { return fuzzy95TM_Words; }
    public void setFuzzy95TM_Words(Long fuzzy95TM_Words) { this.fuzzy95TM_Words = fuzzy95TM_Words; }

    public Long getFuzzy95TM_Characters() { return fuzzy95TM_Characters; }
    public void setFuzzy95TM_Characters(Long fuzzy95TM_Characters) { this.fuzzy95TM_Characters = fuzzy95TM_Characters; }

    public Long getFuzzy95TM_Segments() { return fuzzy95TM_Segments; }
    public void setFuzzy95TM_Segments(Long fuzzy95TM_Segments) { this.fuzzy95TM_Segments = fuzzy95TM_Segments; }

    public Long getFuzzy85TM_Words() { return fuzzy85TM_Words; }
    public void setFuzzy85TM_Words(Long fuzzy85TM_Words) { this.fuzzy85TM_Words = fuzzy85TM_Words; }

    public Long getFuzzy85TM_Characters() { return fuzzy85TM_Characters; }
    public void setFuzzy85TM_Characters(Long fuzzy85TM_Characters) { this.fuzzy85TM_Characters = fuzzy85TM_Characters; }

    public Long getFuzzy85TM_Segments() { return fuzzy85TM_Segments; }
    public void setFuzzy85TM_Segments(Long fuzzy85TM_Segments) { this.fuzzy85TM_Segments = fuzzy85TM_Segments; }

    public Long getFuzzy75TM_Words() { return fuzzy75TM_Words; }
    public void setFuzzy75TM_Words(Long fuzzy75TM_Words) { this.fuzzy75TM_Words = fuzzy75TM_Words; }

    public Long getFuzzy75TM_Characters() { return fuzzy75TM_Characters; }
    public void setFuzzy75TM_Characters(Long fuzzy75TM_Characters) { this.fuzzy75TM_Characters = fuzzy75TM_Characters; }

    public Long getFuzzy75TM_Segments() { return fuzzy75TM_Segments; }
    public void setFuzzy75TM_Segments(Long fuzzy75TM_Segments) { this.fuzzy75TM_Segments = fuzzy75TM_Segments; }

    public Long getFuzzy50TM_Words() { return fuzzy50TM_Words; }
    public void setFuzzy50TM_Words(Long fuzzy50TM_Words) { this.fuzzy50TM_Words = fuzzy50TM_Words; }

    public Long getFuzzy50TM_Characters() { return fuzzy50TM_Characters; }
    public void setFuzzy50TM_Characters(Long fuzzy50TM_Characters) { this.fuzzy50TM_Characters = fuzzy50TM_Characters; }

    public Long getFuzzy50TM_Segments() { return fuzzy50TM_Segments; }
    public void setFuzzy50TM_Segments(Long fuzzy50TM_Segments) { this.fuzzy50TM_Segments = fuzzy50TM_Segments; }

    public Long getNoMatchTM_Words() { return noMatchTM_Words; }
    public void setNoMatchTM_Words(Long noMatchTM_Words) { this.noMatchTM_Words = noMatchTM_Words; }

    public Long getNoMatchTM_Characters() { return noMatchTM_Characters; }
    public void setNoMatchTM_Characters(Long noMatchTM_Characters) { this.noMatchTM_Characters = noMatchTM_Characters; }

    public Long getNoMatchTM_Segments() { return noMatchTM_Segments; }
    public void setNoMatchTM_Segments(Long noMatchTM_Segments) { this.noMatchTM_Segments = noMatchTM_Segments; }

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
}
