package com.tms.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.tms.backend.jobAnalysis.JobAnalysis;
import com.tms.backend.jobAnalysis.JobAnalysisFile;
import com.tms.backend.jobAnalysis.JobAnalysisType;

public record JobAnalysisResponseDTO(
    Long id,
    String tomatoJobId,
    String status,
    String name,
    JobAnalysisType type,
    String sourceLang,
    Set<String> targetLanguages,
    LocalDateTime createDate,
    String createdBy,
    Long approvedSegments, Long approvedWords, Long approvedCharacters, Double approvedWeighted, Double approvedPercentage,
    Long repetitionSegments, Long repetitionWords, Long repetitionCharacters, Double repetitionWeighted, Double repetitionPercentage,
    Long context101Segments, Long context101Words, Long context101Characters, Double context101Weighted, Double context101Percentage,
    Long perfect100Segments, Long perfect100Words, Long perfect100Characters, Double perfect100Weighted, Double perfect100Percentage,
    Long fuzzy95Segments, Long fuzzy95Words, Long fuzzy95Characters, Double fuzzy95Weighted, Double fuzzy95Percentage,
    Long fuzzy85Segments, Long fuzzy85Words, Long fuzzy85Characters, Double fuzzy85Weighted, Double fuzzy85Percentage,
    Long fuzzy75Segments, Long fuzzy75Words, Long fuzzy75Characters, Double fuzzy75Weighted, Double fuzzy75Percentage,
    Long fuzzy50Segments, Long fuzzy50Words, Long fuzzy50Characters, Double fuzzy50Weighted, Double fuzzy50Percentage,
    Long noMatchSegments, Long noMatchWords, Long noMatchCharacters, Double noMatchWeighted, Double noMatchPercentage,
    Long totalSegments, Long totalWords, Long totalCharacters, Double totalWeighted, Double totalWeightedPercentage,
    String unitType,
    List<String> tmNames,
    List<FileAnalysis> files
) {
    public record FileAnalysis(
        String fileName,
        Long approvedSegments, Long approvedWords, Long approvedCharacters, Double approvedWeighted, Double approvedPercentage,
        Long repetitionSegments, Long repetitionWords, Long repetitionCharacters, Double repetitionWeighted, Double repetitionPercentage,
        Long context101Segments, Long context101Words, Long context101Characters, Double context101Weighted, Double context101Percentage,
        Long perfect100Segments, Long perfect100Words, Long perfect100Characters, Double perfect100Weighted, Double perfect100Percentage,
        Long fuzzy95Segments, Long fuzzy95Words, Long fuzzy95Characters, Double fuzzy95Weighted, Double fuzzy95Percentage,
        Long fuzzy85Segments, Long fuzzy85Words, Long fuzzy85Characters, Double fuzzy85Weighted, Double fuzzy85Percentage,
        Long fuzzy75Segments, Long fuzzy75Words, Long fuzzy75Characters, Double fuzzy75Weighted, Double fuzzy75Percentage,
        Long fuzzy50Segments, Long fuzzy50Words, Long fuzzy50Characters, Double fuzzy50Weighted, Double fuzzy50Percentage,
        Long noMatchSegments, Long noMatchWords, Long noMatchCharacters, Double noMatchWeighted, Double noMatchPercentage,
        Long totalSegments, Long totalWords, Long totalCharacters, Double totalWeighted, Double totalWeightedPercentage
    ) {
        public static FileAnalysis fromEntity(JobAnalysisFile f) {
            return new FileAnalysis(
                f.getFileName(),
                f.getApprovedSegments(), f.getApprovedWords(), f.getApprovedCharacters(), f.getApprovedWeighted(), f.getApprovedPercentage(),
                f.getRepetitionSegments(), f.getRepetitionWords(), f.getRepetitionCharacters(), f.getRepetitionWeighted(), f.getRepetitionPercentage(),
                f.getContext101Segments(), f.getContext101Words(), f.getContext101Characters(), f.getContext101Weighted(), f.getContext101Percentage(),
                f.getPerfect100Segments(), f.getPerfect100Words(), f.getPerfect100Characters(), f.getPerfect100Weighted(), f.getPerfect100Percentage(),
                f.getFuzzy95Segments(), f.getFuzzy95Words(), f.getFuzzy95Characters(), f.getFuzzy95Weighted(), f.getFuzzy95Percentage(),
                f.getFuzzy85Segments(), f.getFuzzy85Words(), f.getFuzzy85Characters(), f.getFuzzy85Weighted(), f.getFuzzy85Percentage(),
                f.getFuzzy75Segments(), f.getFuzzy75Words(), f.getFuzzy75Characters(), f.getFuzzy75Weighted(), f.getFuzzy75Percentage(),
                f.getFuzzy50Segments(), f.getFuzzy50Words(), f.getFuzzy50Characters(), f.getFuzzy50Weighted(), f.getFuzzy50Percentage(),
                f.getNoMatchSegments(), f.getNoMatchWords(), f.getNoMatchCharacters(), f.getNoMatchWeighted(), f.getNoMatchPercentage(),
                f.getTotalSegments(), f.getTotalWords(), f.getTotalCharacters(), f.getTotalWeighted(), f.getTotalWeightedPercentage()
            );
        }
    }

    public static JobAnalysisResponseDTO fromEntity(JobAnalysis a) {
        List<FileAnalysis> files = a.getFiles() != null && !a.getFiles().isEmpty()
                ? a.getFiles().stream().map(FileAnalysis::fromEntity).collect(java.util.stream.Collectors.toList())
                : null;

        return new JobAnalysisResponseDTO(
            a.getId(),
            a.getTomatoJobId(),
            "completed",
            a.getName(),
            a.getType(),
            a.getSourceLang(),
            a.getTargetLanguages(),
            a.getCreateDate(),
            a.getCreatedBy(),
            a.getApprovedSegments(), a.getApprovedWords(), a.getApprovedCharacters(), a.getApprovedWeighted(), a.getApprovedPercentage(),
            a.getRepetitionSegments(), a.getRepetitionWords(), a.getRepetitionCharacters(), a.getRepetitionWeighted(), a.getRepetitionPercentage(),
            a.getContext101Segments(), a.getContext101Words(), a.getContext101Characters(), a.getContext101Weighted(), a.getContext101Percentage(),
            a.getPerfect100Segments(), a.getPerfect100Words(), a.getPerfect100Characters(), a.getPerfect100Weighted(), a.getPerfect100Percentage(),
            a.getFuzzy95Segments(), a.getFuzzy95Words(), a.getFuzzy95Characters(), a.getFuzzy95Weighted(), a.getFuzzy95Percentage(),
            a.getFuzzy85Segments(), a.getFuzzy85Words(), a.getFuzzy85Characters(), a.getFuzzy85Weighted(), a.getFuzzy85Percentage(),
            a.getFuzzy75Segments(), a.getFuzzy75Words(), a.getFuzzy75Characters(), a.getFuzzy75Weighted(), a.getFuzzy75Percentage(),
            a.getFuzzy50Segments(), a.getFuzzy50Words(), a.getFuzzy50Characters(), a.getFuzzy50Weighted(), a.getFuzzy50Percentage(),
            a.getNoMatchSegments(), a.getNoMatchWords(), a.getNoMatchCharacters(), a.getNoMatchWeighted(), a.getNoMatchPercentage(),
            a.getTotalSegments(), a.getTotalWords(), a.getTotalCharacters(), a.getTotalWeighted(), a.getTotalWeightedPercentage(),
            a.getUnitType(),
            a.getTmNames(),
            files
        );
    }

    public static JobAnalysisResponseDTO pending(String tomatoJobId) {
        return new JobAnalysisResponseDTO(
            null, tomatoJobId, "pending",
            null, null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null,
            null,
            null,
            null
        );
    }
}
