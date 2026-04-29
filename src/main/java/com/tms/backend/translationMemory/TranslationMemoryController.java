package com.tms.backend.translationMemory;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tm")
public class TranslationMemoryController {

    private final TranslationMemoryService tmService;

    public TranslationMemoryController(TranslationMemoryService tmService) {
        this.tmService = tmService;
    }

    @PostMapping("/{id}/import-tmx")
    public ResponseEntity<String> importTmx(
            @PathVariable Long id,
            @RequestParam MultipartFile file) throws IOException {
        return tmService.importTmx(id, file);
    }
}
