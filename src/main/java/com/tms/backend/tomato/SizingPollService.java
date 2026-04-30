package com.tms.backend.tomato;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.tms.backend.dto.TomatoSizingResponse;
import com.tms.backend.jobAnalysis.JobAnalysisService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SizingPollService {

    private static final Logger log = LoggerFactory.getLogger(SizingPollService.class);
    private static final int MAX_POLL_ATTEMPTS = 60;
    private static final long POLL_INTERVAL_MS = 5_000L;

    private final SizingService sizingService;

    @Lazy
    @Autowired
    private JobAnalysisService jobAnalysisService;

    private final Map<String, String> statusMap = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    private final Map<String, String> errorMap = new ConcurrentHashMap<>();

    /**
     * Starts a background polling loop for the given Tomato jobId.
     * When completed, saves the JobAnalysis to DB and notifies the frontend via SSE.
     */
    @Async
    public void startPolling(String tomatoJobId) {
        statusMap.put(tomatoJobId, "pending");

        try {
            for (int attempt = 1; attempt <= MAX_POLL_ATTEMPTS; attempt++) {
                Thread.sleep(POLL_INTERVAL_MS);

                TomatoSizingResponse result = sizingService.fetchSizingResultOnce(tomatoJobId);

                if (result != null) {
                    log.info("Sizing job {} completed on attempt {}", tomatoJobId, attempt);
                    jobAnalysisService.finalizeJobAnalysis(tomatoJobId, result);
                    statusMap.put(tomatoJobId, "completed");
                    sendSseEvent(tomatoJobId, "completed");
                    return;
                }

                log.info("Sizing job {} still processing (attempt {}/{})", tomatoJobId, attempt, MAX_POLL_ATTEMPTS);
            }

            statusMap.put(tomatoJobId, "failed");
            errorMap.put(tomatoJobId, "Timed out after " + MAX_POLL_ATTEMPTS + " attempts");
            sendSseEvent(tomatoJobId, "failed");
            log.warn("Sizing job {} timed out", tomatoJobId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            statusMap.put(tomatoJobId, "failed");
            errorMap.put(tomatoJobId, "Polling interrupted");
            sendSseEvent(tomatoJobId, "failed");
        } catch (Exception e) {
            statusMap.put(tomatoJobId, "failed");
            errorMap.put(tomatoJobId, e.getMessage());
            sendSseEvent(tomatoJobId, "failed");
            log.error("Sizing job {} failed during polling: {}", tomatoJobId, e.getMessage());
        }
    }

    /**
     * Registers an SSE emitter for the given jobId.
     * If the job already completed before the frontend connected, sends the event immediately.
     */
    public void registerEmitter(String tomatoJobId, SseEmitter emitter) {
        String currentStatus = statusMap.getOrDefault(tomatoJobId, "pending");

        if ("completed".equals(currentStatus) || "failed".equals(currentStatus)) {
            sendAndComplete(emitter, currentStatus);
            return;
        }

        emitterMap.put(tomatoJobId, emitter);
        emitter.onCompletion(() -> emitterMap.remove(tomatoJobId));
        emitter.onTimeout(() -> emitterMap.remove(tomatoJobId));
    }

    private void sendSseEvent(String tomatoJobId, String status) {
        SseEmitter emitter = emitterMap.remove(tomatoJobId);
        if (emitter != null) {
            sendAndComplete(emitter, status);
        }
    }

    private void sendAndComplete(SseEmitter emitter, String status) {
        try {
            emitter.send(SseEmitter.event()
                    .name("sizing-status")
                    .data("{\"status\":\"" + status + "\"}"));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    public String getStatus(String tomatoJobId) {
        return statusMap.getOrDefault(tomatoJobId, "unknown");
    }

    public void cleanup(String tomatoJobId) {
        statusMap.remove(tomatoJobId);
        emitterMap.remove(tomatoJobId);
        errorMap.remove(tomatoJobId);
    }
}
