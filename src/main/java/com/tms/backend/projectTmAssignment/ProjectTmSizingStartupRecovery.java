package com.tms.backend.projectTmAssignment;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ProjectTmSizingStartupRecovery {

    private static final Logger log = LoggerFactory.getLogger(ProjectTmSizingStartupRecovery.class);

    private final ProjectTmSizingRepository sizingRepo;
    private final ProjectTmSizingService sizingService;

    public ProjectTmSizingStartupRecovery(
            ProjectTmSizingRepository sizingRepo, ProjectTmSizingService sizingService) {
        this.sizingRepo = sizingRepo;
        this.sizingService = sizingService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resumeInterruptedJobs() {
        List<ProjectTmSizing> active = sizingRepo.findByStatus("SIZING");
        if (!active.isEmpty()) {
            log.info("Resuming {} interrupted project TM sizing job(s) after restart", active.size());
            for (ProjectTmSizing sizing : active) {
                sizingService.resumeTracking(sizing);
            }
        }
    }
}
