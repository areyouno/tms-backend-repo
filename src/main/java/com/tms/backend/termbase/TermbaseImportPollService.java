package com.tms.backend.termbase;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.tms.backend.dto.TermbaseImportJobStatusDTO;

@Service
public class TermbaseImportPollService {

    private static final Logger log = LoggerFactory.getLogger(TermbaseImportPollService.class);
    private static final int MAX_POLL_ATTEMPTS = 360;
    private static final long POLL_INTERVAL_MS = 5_000L;

    private final TermbaseService termbaseService;
    private final TermbaseImportJobRepository jobRepo;
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    public TermbaseImportPollService(TermbaseService termbaseService, TermbaseImportJobRepository jobRepo) {
        this.termbaseService = termbaseService;
        this.jobRepo = jobRepo;
    }

    @Async
    public void startPolling(String jobId, Long termbaseId) {
        TermbaseImportJob job = jobRepo.findById(jobId).orElseGet(() ->
                jobRepo.save(TermbaseImportJob.pending(jobId, termbaseId)));
        job.setStatus("in_progress");
        job.setUpdatedAt(LocalDateTime.now());
        jobRepo.save(job);

        try {
            for (int attempt = 1; attempt <= MAX_POLL_ATTEMPTS; attempt++) {
                Thread.sleep(POLL_INTERVAL_MS);

                TermbaseImportJobStatusDTO status = termbaseService.fetchImportStatusOnce(jobId);
                if (status == null) continue;

                job.setProgressPercent(status.progressPercent());
                job.setUpdatedAt(LocalDateTime.now());
                jobRepo.save(job);

                sendProgressEvent(jobId, status.progressPercent());

                if (status.isCompleted()) {
                    String finalStatus = status.errorMessage() != null ? "failed" : "completed";
                    job.setStatus(finalStatus);
                    job.setErrorMessage(status.errorMessage());
                    job.setUpdatedAt(LocalDateTime.now());
                    jobRepo.save(job);
                    sendFinalEvent(jobId, finalStatus);
                    log.info("Termbase import job {} {}", jobId, finalStatus);
                    return;
                }

                log.info("Termbase import job {} progress: {}%", jobId, status.progressPercent());
            }

            job.setStatus("failed");
            job.setUpdatedAt(LocalDateTime.now());
            jobRepo.save(job);
            sendFinalEvent(jobId, "failed");
            log.warn("Termbase import job {} timed out", jobId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markFailed(job);
            sendFinalEvent(jobId, "failed");
        } catch (Exception e) {
            markFailed(job);
            sendFinalEvent(jobId, "failed");
            log.error("Termbase import job {} failed during polling: {}", jobId, e.getMessage());
        }
    }

    private void markFailed(TermbaseImportJob job) {
        job.setStatus("failed");
        job.setUpdatedAt(LocalDateTime.now());
        jobRepo.save(job);
    }

    public void registerEmitter(String jobId, SseEmitter emitter) {
        String currentStatus = jobRepo.findById(jobId)
                .map(TermbaseImportJob::getStatus)
                .orElse("pending");
        if ("completed".equals(currentStatus) || "failed".equals(currentStatus)) {
            sendAndComplete(emitter, currentStatus);
            return;
        }
        emitterMap.put(jobId, emitter);
        emitter.onCompletion(() -> emitterMap.remove(jobId));
        emitter.onTimeout(() -> emitterMap.remove(jobId));
    }

    private void sendProgressEvent(String jobId, Double progressPercent) {
        SseEmitter emitter = emitterMap.get(jobId);
        if (emitter == null) return;
        try {
            String data = String.format("{\"progressPercent\":%.2f}",
                    progressPercent != null ? progressPercent : 0.0);
            emitter.send(SseEmitter.event().name("import-progress").data(data));
        } catch (IOException e) {
            emitterMap.remove(jobId);
        }
    }

    private void sendFinalEvent(String jobId, String status) {
        SseEmitter emitter = emitterMap.remove(jobId);
        if (emitter != null) {
            sendAndComplete(emitter, status);
        }
    }

    private void sendAndComplete(SseEmitter emitter, String status) {
        try {
            emitter.send(SseEmitter.event()
                    .name("import-status")
                    .data("{\"status\":\"" + status + "\"}"));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
