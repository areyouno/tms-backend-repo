package com.tms.backend.translationMemory;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tms.backend.dto.ImportTmxRequestDTO;

@RestController
@RequestMapping("/api/tm")
public class TranslationMemoryController {

    private final TranslationMemoryService tmService;

    public TranslationMemoryController(TranslationMemoryService tmService) {
        this.tmService = tmService;
    }

    @PostMapping(value = "/{id}/import-tmx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importTmx(
            @PathVariable Long id,
            @RequestPart("tmxFile") MultipartFile file,
            @RequestPart("metadata") ImportTmxRequestDTO metadata) throws IOException {
        return tmService.importTmx(id, file, metadata);
    }
}
