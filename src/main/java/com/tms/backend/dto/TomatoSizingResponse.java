package com.tms.backend.dto;

public record TomatoSizingResponse(Statistics statistics){
    public record Statistics(
        Long totalCount,
        Long totalSegments,
        Long totalWords,
        Long totalCharacters
    ) {}
}
