package com.tms.backend.projectTmAssignment;

import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.tms.backend.dto.NetRateSchemeResponseDTO;
import com.tms.backend.dto.TemplateSizingResultResponse.TemplateTmInfo;
import com.tms.backend.netRateScheme.NetRateSchemeService;
import com.tms.backend.project.Project;
import com.tms.backend.project.ProjectRepository;
import com.tms.backend.tomato.SizingService;
import com.tms.backend.tomato.TemplateSizingPollStatus;

import jakarta.persistence.EntityNotFoundException;

/**
 * Submits a Tomato template-TM sizing job per project + language pair whenever TMs are
 * assigned to a project (see ProjectTmAssignmentService.assignTMs), then polls until Tomato
 * returns the resulting template TM. Progress/result is recorded on ProjectTmSizing.
 */
@Service
public class ProjectTmSizingService {

    private static final Logger log = LoggerFactory.getLogger(ProjectTmSizingService.class);
    private static final int MAX_POLL_ATTEMPTS = 60;
    private static final long POLL_INTERVAL_MS = 5_000L;
    private static final int DEFAULT_MIN_SIMILARITY = 75;

    private final ProjectTmSizingRepository sizingRepo;
    private final ProjectRepository projectRepo;
    private final NetRateSchemeService netRateSchemeService;
    private final SizingService sizingService;
    private final PlatformTransactionManager transactionManager;

    public ProjectTmSizingService(
            ProjectTmSizingRepository sizingRepo,
            ProjectRepository projectRepo,
            NetRateSchemeService netRateSchemeService,
            SizingService sizingService,
            PlatformTransactionManager transactionManager) {
        this.sizingRepo = sizingRepo;
        this.projectRepo = projectRepo;
        this.netRateSchemeService = netRateSchemeService;
        this.sizingService = sizingService;
        this.transactionManager = transactionManager;
    }

    /**
     * Entry point called once a batch of TM assignments for a single (project, source lang,
     * target lang) group has been committed. Must only be invoked after that commit, since this
     * runs on its own thread/transactions.
     */
    @Async
    public void submitAndTrackSizing(Long projectId, String sourceLang, String targetLang, List<Long> tmIds,
            List<String> filePaths, String templateTmName, String requestingUsername) {

        Context ctx = inTx(() -> {
            Project project = projectRepo.findById(projectId)
                    .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

            ProjectTmSizing sizing = new ProjectTmSizing();
            sizing.setProject(project);
            sizing.setSourceLang(sourceLang);
            sizing.setTargetLang(targetLang);
            sizing.setTmIds(tmIds);
            sizing.setTemplateTmName(templateTmName);
            sizing.setRequestedByUsername(requestingUsername);
            sizing.setStatus("SIZING");
            Long sizingId = sizingRepo.save(sizing).getId();

            NetRateSchemeResponseDTO scheme = netRateSchemeService.resolveSchemeForProject(project);
            String sizingRequestJson = netRateSchemeService.buildSizingRequestJson(scheme, projectId);
            return new Context(sizingId, sizingRequestJson);
        });

        String sizingJobId;
        try {
            sizingJobId = sizingService.submitTemplateSizing(filePaths, templateTmName, requestingUsername, tmIds,
                    sourceLang, targetLang, ctx.sizingRequestJson(), DEFAULT_MIN_SIMILARITY, null);
        } catch (Exception e) {
            log.error("Failed to submit TM sizing for project {} ({} -> {}): {}",
                    projectId, sourceLang, targetLang, e.getMessage());
            markFailed(ctx.sizingId(), "Failed to submit sizing job: " + e.getMessage());
            return;
        }

        inTxVoid(() -> {
            ProjectTmSizing sizing = sizingRepo.findById(ctx.sizingId()).orElseThrow();
            sizing.setSizingJobId(sizingJobId);
            sizingRepo.save(sizing);
        });

        pollAndStore(ctx.sizingId(), sizingJobId);
    }

    private record Context(Long sizingId, String sizingRequestJson) {}

    /**
     * Resumes polling for a sizing job left mid-way by a server restart (see
     * ProjectTmSizingStartupRecovery). If no sizing job was ever recorded (the row was created
     * but submitTemplateSizing hadn't returned yet), there's nothing to poll, so it's marked
     * failed instead.
     */
    @Async
    public void resumeTracking(ProjectTmSizing sizing) {
        if (sizing.getSizingJobId() == null) {
            markFailed(sizing.getId(), "No sizing job id recorded; cannot resume after restart");
            return;
        }
        pollAndStore(sizing.getId(), sizing.getSizingJobId());
    }

    private void pollAndStore(Long sizingId, String sizingJobId) {
        TemplateTmInfo templateTm = null;
        try {
            for (int attempt = 1; attempt <= MAX_POLL_ATTEMPTS; attempt++) {
                Thread.sleep(POLL_INTERVAL_MS);

                TemplateSizingPollStatus pollStatus = sizingService.fetchTemplateSizingResultOnce(sizingJobId);
                if (pollStatus.completed()) {
                    templateTm = pollStatus.templateTm();
                    break;
                }
                log.info("TM sizing job {} still processing (attempt {}/{})",
                        sizingJobId, attempt, MAX_POLL_ATTEMPTS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markFailed(sizingId, "Polling interrupted");
            return;
        } catch (Exception e) {
            log.error("TM sizing job {} failed: {}", sizingJobId, e.getMessage());
            markFailed(sizingId, "Sizing failed: " + e.getMessage());
            return;
        }

        if (templateTm == null) {
            markFailed(sizingId, "Timed out waiting for sizing result");
            return;
        }

        final TemplateTmInfo resolved = templateTm;
        inTxVoid(() -> {
            ProjectTmSizing sizing = sizingRepo.findById(sizingId).orElseThrow();
            sizing.setTemplateTmId(resolved.tmId());
            sizing.setTemplateTmName(resolved.name());
            sizing.setTemplateTmUnitCount(resolved.unitCount());
            sizing.setStatus("COMPLETED");
            sizingRepo.save(sizing);
        });
    }

    private void markFailed(Long sizingId, String error) {
        inTxVoid(() -> sizingRepo.findById(sizingId).ifPresent(sizing -> {
            sizing.setStatus("FAILED");
            sizing.setErrorMessage(error);
            sizingRepo.save(sizing);
        }));
    }

    private <T> T inTx(Supplier<T> action) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        return tt.execute(status -> action.get());
    }

    private void inTxVoid(Runnable action) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.executeWithoutResult(status -> action.run());
    }
}
