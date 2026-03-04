package com.tms.backend.dto;

public record CatEditorSettingDTO(
   boolean displayNonTranslatablesScoresInCatEditor,
   boolean suggestMtOnlyForSegmentsWithTmMatchBelow,
   boolean displayPhraseQualityPerformanceScoreMatchesInCatEditor,
   boolean autoPropagateRepetitions,
   boolean autoPropagateToLockedRepetitions,
   boolean setSegmentStatusConfirmedForRepetitions,
   boolean lockSubsequentRepetitions
) {}