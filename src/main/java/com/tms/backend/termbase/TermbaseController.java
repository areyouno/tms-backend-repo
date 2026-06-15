package com.tms.backend.termbase;

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

@RestController
@RequestMapping("/api/termbase")
public class TermbaseController {

    private final TermbaseService termbaseService;
    private final TermbaseImportPollService pollService;
    private final TermbaseImportJobRepository jobRepo;

    public TermbaseController(TermbaseService termbaseService, TermbaseImportPollService pollService, TermbaseImportJobRepository jobRepo) {
        this.termbaseService = termbaseService;
        this.pollService = pollService;
        this.jobRepo = jobRepo;
    }

    @PostMapping(value = "/{id}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> importTermbase(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @RequestPart("userName") String userName) throws IOException {
        String jobId = termbaseService.submitImport(id, file, userName);
        pollService.startPolling(jobId, id);
        return ResponseEntity.accepted().body(Map.of("jobId", jobId));
    }

    @GetMapping(value = "/import/jobs/{tomatoJobId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamImportStatus(@PathVariable String jobId) {
        SseEmitter emitter = new SseEmitter(1_800_000L);
        pollService.registerEmitter(jobId, emitter);
        return emitter;
    }

    @GetMapping("/import/jobs/{jobId}")
    public ResponseEntity<TermbaseImportJob> getImportJobStatus(@PathVariable String jobId) {
        return jobRepo.findById(jobId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
