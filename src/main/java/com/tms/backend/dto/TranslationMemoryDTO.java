package com.tms.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record TranslationMemoryDTO(
        Long id,
        String name,
        String client,
        @JsonAlias({ "sourceLang", "sourceLocale" }) String sourceLanguage,
        @JsonAlias({ "targetLang" }) String targetLanguage,
        String domain,
        String status,
        String file
) {}
