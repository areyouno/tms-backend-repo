package com.tms.backend.translationMemory;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TmxImportStartupRecovery {

    private static final Logger log = LoggerFactory.getLogger(TmxImportStartupRecovery.class);

    private final TmxImportJobRepository jobRepo;
    private final TmxImportPollService pollService;

    public TmxImportStartupRecovery(TmxImportJobRepository jobRepo, TmxImportPollService pollService) {
        this.jobRepo = jobRepo;
        this.pollService = pollService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resumeInterruptedJobs() {
        List<TmxImportJob> active = jobRepo.findByStatusIn(List.of("pending", "in_progress"));
        if (!active.isEmpty()) {
            log.info("Resuming {} interrupted TMX import job(s) after restart", active.size());
            for (TmxImportJob job : active) {
                pollService.startPolling(job.getJobId(), job.getTmId(), job.getUserName());
            }
        }
    }
}
