package com.tms.backend.translationMemory;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tms.backend.dto.TmCleanupResponseDTO;

// Mirrors Tomato's own /api/TM path casing (see TranslationMemoryService, which proxies to
// Tomato under the same convention) rather than this app's internal lowercase /api/tm routes.
@RestController
@RequestMapping("/api/TM")
public class TmCleanupController {

    private final TranslationMemoryService tmService;

    public TmCleanupController(TranslationMemoryService tmService) {
        this.tmService = tmService;
    }

    @PostMapping(value = "/{id}/cleanup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TmCleanupResponseDTO> cleanup(
            @PathVariable Long id,
            @RequestParam("xliffFile") MultipartFile xliffFile,
            @RequestParam("userName") String userName) throws IOException {
        return ResponseEntity.ok(tmService.cleanupTm(id, xliffFile, userName));
    }
}
