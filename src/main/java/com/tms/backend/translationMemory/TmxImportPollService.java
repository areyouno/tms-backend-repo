package com.tms.backend.translationMemory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.tms.backend.dto.TmxImportJobStatusDTO;

@Service
public class TmxImportPollService {

    private static final Logger log = LoggerFactory.getLogger(TmxImportPollService.class);
    private static final int MAX_POLL_ATTEMPTS = 360;
    private static final long POLL_INTERVAL_MS = 5_000L;

    private final TranslationMemoryService tmService;

    private final Map<String, String> statusMap = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    public TmxImportPollService(TranslationMemoryService tmService) {
        this.tmService = tmService;
    }

    @Async
    public void startPolling(String jobId) {
        statusMap.put(jobId, "pending");
        try {
            for (int attempt = 1; attempt <= MAX_POLL_ATTEMPTS; attempt++) {
                Thread.sleep(POLL_INTERVAL_MS);

                TmxImportJobStatusDTO status = tmService.fetchImportStatusOnce(jobId);
                if (status == null) continue;

                sendProgressEvent(jobId, status);

                if (status.isCompleted()) {
                    String finalStatus = status.errorMessage() != null ? "failed" : "completed";
                    statusMap.put(jobId, finalStatus);
                    sendFinalEvent(jobId, finalStatus);
                    log.info("TMX import job {} {}", jobId, finalStatus);
                    return;
                }

                log.info("TMX import job {} progress: {}%", jobId, status.progressPercent());
            }

            statusMap.put(jobId, "failed");
            sendFinalEvent(jobId, "failed");
            log.warn("TMX import job {} timed out", jobId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            statusMap.put(jobId, "failed");
            sendFinalEvent(jobId, "failed");
        } catch (Exception e) {
            statusMap.put(jobId, "failed");
            sendFinalEvent(jobId, "failed");
            log.error("TMX import job {} failed during polling: {}", jobId, e.getMessage());
        }
    }

    public void registerEmitter(String jobId, SseEmitter emitter) {
        String currentStatus = statusMap.getOrDefault(jobId, "pending");
        if ("completed".equals(currentStatus) || "failed".equals(currentStatus)) {
            sendAndComplete(emitter, currentStatus);
            return;
        }
        emitterMap.put(jobId, emitter);
        emitter.onCompletion(() -> emitterMap.remove(jobId));
        emitter.onTimeout(() -> emitterMap.remove(jobId));
    }

    private void sendProgressEvent(String jobId, TmxImportJobStatusDTO status) {
        SseEmitter emitter = emitterMap.get(jobId);
        if (emitter == null) return;
        try {
            String data = String.format(
                    "{\"progressPercent\":%.2f,\"processedCount\":%d,\"totalCount\":%d,\"importedCount\":%d,\"skippedCount\":%d}",
                    status.progressPercent() != null ? status.progressPercent() : 0.0,
                    status.processedCount() != null ? status.processedCount() : 0,
                    status.totalCount() != null ? status.totalCount() : 0,
                    status.importedCount() != null ? status.importedCount() : 0,
                    status.skippedCount() != null ? status.skippedCount() : 0
            );
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

    public String getStatus(String jobId) {
        return statusMap.getOrDefault(jobId, "unknown");
    }

    public void cleanup(String jobId) {
        statusMap.remove(jobId);
        emitterMap.remove(jobId);
    }
}
