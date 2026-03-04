package com.tms.backend.dto;

public record TomatoSizingResponse(Statistics statistics){
    public record Statistics(
        Long approvedCount,
        Long approvedSegments,
        Double approvedPercentage,
        Long repetitionCount,
        Long repetitionSegments,
        Double repetitionPercentage,
        Long contextMatchCount,
        Long contextMatchSegments,
        Double contextMatchPercentage,
        Long perfect100Count,
        Long perfect100Segments,
        Double perfect100Percentage,
        Long fuzzy95Count,
        Long fuzzy95Segments,
        Double fuzzy95Percentage,
        Long fuzzy85Count,
        Long fuzzy85Segments,
        Double fuzzy85Percentage,
        Long fuzzy75Count,
        Long fuzzy75Segments,
        Double fuzzy75Percentage,
        Long fuzzy50Count,
        Long fuzzy50Segments,
        Double fuzzy50Percentage,
        Long noMatchCount,
        Long noMatchSegments,
        Double noMatchPercentage,
        Long totalCount,
        Long totalSegments,
        Long totalWords,
        Long totalCharacters
    ) {}
}
