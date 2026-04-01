package com.tms.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.tms.backend.jobAnalysis.JobAnalysis;
import com.tms.backend.jobAnalysis.JobAnalysisFile;
import com.tms.backend.jobAnalysis.JobAnalysisType;

public record JobAnalysisResponseDTO(
    Long id,
    String name,
    JobAnalysisType type,
    String sourceLang,
    Set<String> targetLanguages,
    LocalDateTime createDate,
    String createdBy,
    Long repetitionWords,
    Long repetitionCharacters,
    Long repetitionSegments,
    Long contextMatchWords,
    Long contextMatchCharacters,
    Long contextMatchSegments,
    Long perfect100Words,
    Long perfect100Characters,
    Long perfect100Segments,
    Long fuzzy95Words,
    Long fuzzy95Characters,
    Long fuzzy95Segments,
    Long fuzzy85Words,
    Long fuzzy85Characters,
    Long fuzzy85Segments,
    Long fuzzy75Words,
    Long fuzzy75Characters,
    Long fuzzy75Segments,
    Long fuzzy50Words,
    Long fuzzy50Characters,
    Long fuzzy50Segments,
    Long noMatchWords,
    Long noMatchCharacters,
    Long noMatchSegments,
    List<FileAnalysis> files
) {
    public record FileAnalysis(
        String fileName,
        Long approvedTM_Words, Long approvedTM_Characters, Long approvedTM_Segments,
        Long repetitionTM_Words, Long repetitionTM_Characters, Long repetitionTM_Segments,
        Long context101TM_Words, Long context101TM_Characters, Long context101TM_Segments,
        Long perfect100TM_Words, Long perfect100TM_Characters, Long perfect100TM_Segments,
        Long fuzzy95TM_Words, Long fuzzy95TM_Characters, Long fuzzy95TM_Segments,
        Long fuzzy85TM_Words, Long fuzzy85TM_Characters, Long fuzzy85TM_Segments,
        Long fuzzy75TM_Words, Long fuzzy75TM_Characters, Long fuzzy75TM_Segments,
        Long fuzzy50TM_Words, Long fuzzy50TM_Characters, Long fuzzy50TM_Segments,
        Long noMatchTM_Words, Long noMatchTM_Characters, Long noMatchTM_Segments
    ) {
        public static FileAnalysis fromEntity(JobAnalysisFile f) {
            return new FileAnalysis(
                f.getFileName(),
                f.getApprovedTM_Words(), f.getApprovedTM_Characters(), f.getApprovedTM_Segments(),
                f.getRepetitionTM_Words(), f.getRepetitionTM_Characters(), f.getRepetitionTM_Segments(),
                f.getContext101TM_Words(), f.getContext101TM_Characters(), f.getContext101TM_Segments(),
                f.getPerfect100TM_Words(), f.getPerfect100TM_Characters(), f.getPerfect100TM_Segments(),
                f.getFuzzy95TM_Words(), f.getFuzzy95TM_Characters(), f.getFuzzy95TM_Segments(),
                f.getFuzzy85TM_Words(), f.getFuzzy85TM_Characters(), f.getFuzzy85TM_Segments(),
                f.getFuzzy75TM_Words(), f.getFuzzy75TM_Characters(), f.getFuzzy75TM_Segments(),
                f.getFuzzy50TM_Words(), f.getFuzzy50TM_Characters(), f.getFuzzy50TM_Segments(),
                f.getNoMatchTM_Words(), f.getNoMatchTM_Characters(), f.getNoMatchTM_Segments()
            );
        }
    }

    public static JobAnalysisResponseDTO fromEntity(JobAnalysis a) {
        List<FileAnalysis> files = a.getFiles() != null && !a.getFiles().isEmpty()
                ? a.getFiles().stream().map(FileAnalysis::fromEntity).collect(java.util.stream.Collectors.toList())
                : null;

        return new JobAnalysisResponseDTO(
            a.getId(),
            a.getName(),
            a.getType(),
            a.getSourceLang(),
            a.getTargetLanguages(),
            a.getCreateDate(),
            a.getCreatedBy(),
            a.getRepetitionWords(),
            a.getRepetitionCharacters(),
            a.getRepetitionSegments(),
            a.getContextMatchWords(),
            a.getContextMatchCharacters(),
            a.getContextMatchSegments(),
            a.getPerfect100Words(),
            a.getPerfect100Characters(),
            a.getPerfect100Segments(),
            a.getFuzzy95Words(),
            a.getFuzzy95Characters(),
            a.getFuzzy95Segments(),
            a.getFuzzy85Words(),
            a.getFuzzy85Characters(),
            a.getFuzzy85Segments(),
            a.getFuzzy75Words(),
            a.getFuzzy75Characters(),
            a.getFuzzy75Segments(),
            a.getFuzzy50Words(),
            a.getFuzzy50Characters(),
            a.getFuzzy50Segments(),
            a.getNoMatchWords(),
            a.getNoMatchCharacters(),
            a.getNoMatchSegments(),
            files
        );
    }
}
