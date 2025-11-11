package com.tms.backend.language;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.LanguageStatusUpdateRequest;

@RestController
@RequestMapping("/api/languages")
public class LanguageController {

    private final LanguageService languageService;

    public LanguageController(LanguageService languageService) {
        this.languageService = languageService;
    }

    @GetMapping
    public ResponseEntity<List<Language>> getAllLanguages() {
        return ResponseEntity.ok(languageService.getAllLanguages());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Language>> getActiveLanguages() {
        return ResponseEntity.ok(languageService.getActiveLanguages());
    }

    @GetMapping("/inactive")
    public ResponseEntity<List<Language>> getInactiveLanguages() {
        return ResponseEntity.ok(languageService.getInactiveLanguages());
    }

    @PutMapping("/activate")
    public ResponseEntity<List<Language>> activateLanguages(@RequestBody List<String> rfcCodes) {
        List<Language> updated = languageService.setActiveStatusForMultiple(rfcCodes, true);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/deactivate")
    public ResponseEntity<List<Language>> deactivateLanguages(@RequestBody List<String> rfcCodes) {
        List<Language> updated = languageService.setActiveStatusForMultiple(rfcCodes, false);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/update-status")
    public ResponseEntity<Map<String, Object>> updateLanguageStatuses(
            @RequestBody LanguageStatusUpdateRequest request) {
        Map<String, Object> result = languageService.updateStatuses(request);
        return ResponseEntity.ok(result);
    }

}
