package com.tms.backend.language;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tms.backend.dto.CountryDTO;
import com.tms.backend.dto.LanguageStatusUpdateRequest;

import jakarta.transaction.Transactional;

@Service
public class LanguageService {

    private final LanguageRepository languageRepository;

    public LanguageService(LanguageRepository languageRepository) {
        this.languageRepository = languageRepository;
    }

    // Get the distinct list of countries represented in the languages table
    public List<CountryDTO> getAvailableCountries() {
        Map<String, String> countriesByCode = new HashMap<>();

        for (String rfcCode : languageRepository.findAllRfcCodes()) {
            Locale locale = Locale.forLanguageTag(rfcCode);
            String countryCode = locale.getCountry();

            // Skip tags with no region, or with UN M49 numeric region codes
            // (e.g. "en-001" for World, "en-029" for Caribbean) which are not countries
            if (!countryCode.matches("[A-Z]{2}")) {
                continue;
            }

            countriesByCode.putIfAbsent(countryCode, locale.getDisplayCountry(Locale.ENGLISH));
        }

        return countriesByCode.entrySet().stream()
                .map(entry -> new CountryDTO(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CountryDTO::name))
                .toList();
    }

    // Get all active languages
    public List<Language> getActiveLanguages() {
        return languageRepository.findByIsActiveTrue();
    }

    // Get all inactive languages
    public List<Language> getInactiveLanguages() {
        return languageRepository.findByIsActiveFalse();
    }

    // Get all languages (active or inactive)
    public List<Language> getAllLanguages() {
        return languageRepository.findAll();
    }

    // Set language active/inactive by RFC code
    @Transactional
    public List<Language> setActiveStatusForMultiple(List<String> rfcCodes, boolean isActive) {
        List<Language> languages = languageRepository.findAllByRfcCodeIn(rfcCodes);

        if (languages.isEmpty()) {
            throw new IllegalArgumentException("No languages found for given RFC codes");
        }

        languages.forEach(lang -> lang.setActive(isActive));
        return languageRepository.saveAll(languages);
    }

    @Transactional
    public Map<String, Object> updateStatuses(LanguageStatusUpdateRequest request) {
        List<Language> activated = new ArrayList<>();
        List<Language> deactivated = new ArrayList<>();

        if (request.getActivate() != null && !request.getActivate().isEmpty()) {
            activated = setActiveStatusForMultiple(request.getActivate(), true);
        }

        if (request.getDeactivate() != null && !request.getDeactivate().isEmpty()) {
            deactivated = setActiveStatusForMultiple(request.getDeactivate(), false);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("activated", activated.stream().map(Language::getRfcCode).toList());
        result.put("deactivated", deactivated.stream().map(Language::getRfcCode).toList());
        result.put("activatedCount", activated.size());
        result.put("deactivatedCount", deactivated.size());

        return result;
    }


    // Activate all languages
    @Transactional
    public void activateAll() {
        List<Language> all = languageRepository.findAll();
        all.forEach(lang -> lang.setActive(true));
        languageRepository.saveAll(all);
    }

    // Deactivate all languages
    @Transactional
    public void deactivateAll() {
        List<Language> all = languageRepository.findAll();
        all.forEach(lang -> lang.setActive(false));
        languageRepository.saveAll(all);
    }
}
