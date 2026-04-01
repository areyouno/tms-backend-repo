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
}
