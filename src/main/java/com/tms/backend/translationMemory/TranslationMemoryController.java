package com.tms.backend.translationMemory;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.tms.backend.dto.ImportTmxRequestDTO;

@RestController
@RequestMapping("/api/tm")
public class TranslationMemoryController {

    private final TranslationMemoryService tmService;
    private final TmxImportPollService pollService;

    public TranslationMemoryController(TranslationMemoryService tmService, TmxImportPollService pollService) {
        this.tmService = tmService;
        this.pollService = pollService;
    }

    @PostMapping(value = "/{id}/import-tmx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> importTmx(
            @PathVariable Long id,
            @RequestPart("tmxFile") MultipartFile file,
            @RequestPart("metadata") ImportTmxRequestDTO metadata) throws IOException {
        String jobId = tmService.submitImportTmx(id, file, metadata);
        pollService.startPolling(jobId);
        return ResponseEntity.accepted().body(Map.of("jobId", jobId));
    }

    @GetMapping(value = "/import-tmx/jobs/{jobId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamImportStatus(@PathVariable String jobId) {
        SseEmitter emitter = new SseEmitter(600_000L);
        pollService.registerEmitter(jobId, emitter);
        return emitter;
    }
}
