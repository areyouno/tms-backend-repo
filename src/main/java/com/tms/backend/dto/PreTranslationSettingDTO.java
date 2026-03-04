package com.tms.backend.dto;

public record PreTranslationSettingDTO(
    boolean overwriteExistingTranslationsInTargetSegments,
    boolean preTranslateOnJobCreation,
    boolean preTranslateFromTranslationMem,
    int tmThreshold,
    boolean preTranslateFromNonTranslatables,
    boolean preTranslateFromMachineTranslation,

    //set segment status to 'confirmed' for
    boolean transMemoryMatch_101Confirmed,
    boolean transMemoryMatch_100Confirmed,
    boolean nonTranslatables_100Confirmed,
    boolean machineTranslationSuggestionsQpsConfirmed,
    int qpsThreshold,

    // set job to completed once
    boolean preTranslated,
    boolean preTranslatedAndAllSegmentsConfirmed,
    // set project to completed once
    boolean allJobsPreTranslated,
    // lock segment for 
    boolean transMemoryMatch_101Lock,
    boolean transMemoryMatch_100Lock,
    boolean nonTranslatables_100Lock,
    boolean machineTranslationSuggestionsQpsLock
) {}
