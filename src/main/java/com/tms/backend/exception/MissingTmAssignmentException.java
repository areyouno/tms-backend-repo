package com.tms.backend.exception;

import java.util.List;

public class MissingTmAssignmentException extends RuntimeException {

    public record MissingLanguagePair(String sourceLanguage, String targetLanguage) {}

    private final List<MissingLanguagePair> languagePairs;

    public MissingTmAssignmentException(List<MissingLanguagePair> languagePairs) {
        super("No TM/s assigned for the following:");
        this.languagePairs = languagePairs;
    }

    public List<MissingLanguagePair> getLanguagePairs() {
        return languagePairs;
    }
}
