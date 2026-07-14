package com.tms.backend.dto;

import java.util.List;

public record TomatoSizingResponse(String status, Statistics statistics, String fileName, List<TomatoSizingResponse> files, String tomatoJobId) {
    public record Statistics(
        Long approvedSegments,
        Long approvedWords,
        Long approvedCharacters,
        Double approvedWeighted,
        Double approvedPercentage,

        Long repetitionSegments,
        Long repetitionWords,
        Long repetitionCharacters,
        Double repetitionWeighted,
        Double repetitionPercentage,

        Long context101Segments,
        Long context101Words,
        Long context101Characters,
        Double context101Weighted,
        Double context101Percentage,

        Long perfect100Segments,
        Long perfect100Words,
        Long perfect100Characters,
        Double perfect100Weighted,
        Double perfect100Percentage,

        Long fuzzy95Segments,
        Long fuzzy95Words,
        Long fuzzy95Characters,
        Double fuzzy95Weighted,
        Double fuzzy95Percentage,

        Long fuzzy85Segments,
        Long fuzzy85Words,
        Long fuzzy85Characters,
        Double fuzzy85Weighted,
        Double fuzzy85Percentage,

        Long fuzzy75Segments,
        Long fuzzy75Words,
        Long fuzzy75Characters,
        Double fuzzy75Weighted,
        Double fuzzy75Percentage,

        Long fuzzy50Segments,
        Long fuzzy50Words,
        Long fuzzy50Characters,
        Double fuzzy50Weighted,
        Double fuzzy50Percentage,

        Long noMatchSegments,
        Long noMatchWords,
        Long noMatchCharacters,
        Double noMatchWeighted,
        Double noMatchPercentage,

        Long totalSegments,
        Long totalWords,
        Long totalCharacters,
        Double totalWeighted,
        Double totalWeightedPercentage,

        String sourceLanguage,
        String targetLanguage,
        String unitType,
        List<String> tmNames
    ) {}
}
