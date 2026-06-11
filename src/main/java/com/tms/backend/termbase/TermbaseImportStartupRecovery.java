package com.tms.backend.termbase;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TermbaseImportStartupRecovery {

    private static final Logger log = LoggerFactory.getLogger(TermbaseImportStartupRecovery.class);

    private final TermbaseImportJobRepository jobRepo;
    private final TermbaseImportPollService pollService;

    public TermbaseImportStartupRecovery(TermbaseImportJobRepository jobRepo, TermbaseImportPollService pollService) {
        this.jobRepo = jobRepo;
        this.pollService = pollService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resumeInterruptedJobs() {
        List<TermbaseImportJob> active = jobRepo.findByStatusIn(List.of("pending", "in_progress"));
        if (!active.isEmpty()) {
            log.info("Resuming {} interrupted termbase import job(s) after restart", active.size());
            for (TermbaseImportJob job : active) {
                pollService.startPolling(job.getJobId(), job.getTermbaseId());
            }
        }
    }
}
