package com.tms.backend.jobAnalysis;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tms.backend.dto.JobAnalysisResponseDTO;
import com.tms.backend.dto.MatchTypeRateResponseDTO;
import com.tms.backend.dto.NetRateSchemeResponseDTO;
import com.tms.backend.dto.TomatoSizingResponse;
import com.tms.backend.dto.WorkflowStepRateResponseDTO;
import com.tms.backend.job.Job;
import com.tms.backend.job.JobRepository;
import com.tms.backend.job.JobWorkflowStep;
import com.tms.backend.job.JobWorkflowStepRepository;
import com.tms.backend.netRateScheme.MatchType;
import com.tms.backend.netRateScheme.NetRateSchemeService;
import com.tms.backend.settingAnalysis.AnalysisSetting;
import com.tms.backend.settingAnalysis.AnalysisSettingService;
import com.tms.backend.tomato.SizingService;
import com.tms.backend.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobAnalysisService {
    private static final Logger log = LoggerFactory.getLogger(JobAnalysisService.class);

    private final JobAnalysisRepository jobAnalysisRepository;
    private final AnalysisSettingService analysisSettingService;
    private final JobRepository jobRepository;
    private final SizingService sizingService;
    private final NetRateSchemeService netRateSchemeService;
    private final JobWorkflowStepRepository jobWorkflowStepRepository;

    /**
     * Creates a new JobAnalysis using the user's default analysis settings.
     * Replaces macros in the name template (e.g., {projectName}) with actual values.
     *
     * @param job The job for which the analysis is being created
     * @param user The user creating the analysis
     * @param languages The set of languages to analyze
     * @return The created JobAnalysis
     */
    @Transactional
    public JobAnalysis createJobAnalysis(Job job, Long workflowStepId, User user, Set<String> languages) {
        // Get the user's analysis setting (or global default)
        AnalysisSetting setting = analysisSettingService.getUserSetting(user);

        // Create new JobAnalysis
        JobAnalysis jobAnalysis = new JobAnalysis();

        // Replace macros in the name template
        String resolvedName = resolveNameMacros(setting.getName(), job, workflowStepId);
        jobAnalysis.setName(resolvedName);

        jobAnalysis.setTargetLanguages(languages);
        jobAnalysis.setCreateDate(LocalDateTime.now());
        jobAnalysis.setCreatedBy(user.getUsername());
        jobAnalysis.setType(JobAnalysisType.DEFAULT);

        // Send file to Tomato API for sizing
        String filePath = job.getConvertedFilePath();
        TomatoSizingResponse sizingResponse = sizingService.sendFileToTomatoAPI(filePath);
        TomatoSizingResponse.Statistics stats = sizingResponse.statistics();

        // Resolve the actual WorkflowStep id from the JobWorkflowStep id
        JobWorkflowStep jobWorkflowStep = jobWorkflowStepRepository.findById(workflowStepId)
                .orElseThrow(() -> new RuntimeException(
                        "JobWorkflowStep not found with id: " + workflowStepId));
        Long actualWorkflowStepId = jobWorkflowStep.getWorkflowStep().getId();
        log.info("Resolved JobWorkflowStep id {} -> WorkflowStep id {}", workflowStepId, actualWorkflowStepId);

        // Get default net rate scheme and find the matching workflow step rates
        NetRateSchemeResponseDTO defaultScheme = netRateSchemeService.getDefaultScheme();
        WorkflowStepRateResponseDTO stepRate = defaultScheme.workflowStepRates().stream()
                .filter(wf -> wf.workflowStepId().equals(actualWorkflowStepId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "No rates found for workflowStepId: " + actualWorkflowStepId));

        // Build a map of MatchType -> transMemoryPercent for quick lookup
        Map<MatchType, Long> transMemPercentMap = stepRate.matchTypeRates().stream()
                .collect(Collectors.toMap(
                        MatchTypeRateResponseDTO::matchType,
                        MatchTypeRateResponseDTO::transMemoryPercent
                ));
        log.info("transMemPercentMap for workflowStepId {}: {}", actualWorkflowStepId, transMemPercentMap);
        log.info("Sizing stats - repetition: {}, contextMatch: {}, perfect100: {}, fuzzy95: {}, fuzzy85: {}, fuzzy75: {}, fuzzy50: {}, noMatch: {}",
                stats.repetitionSegments(), stats.contextMatchSegments(), stats.perfect100Segments(),
                stats.fuzzy95Segments(), stats.fuzzy85Segments(), stats.fuzzy75Segments(),
                stats.fuzzy50Segments(), stats.noMatchSegments());

        // Calculate net rates: segments * (transMemoryPercent / 100)
        Function<Long, Long> safeVal = v -> v != null ? v : 0L;

        double netRepetition = safeVal.apply(stats.repetitionSegments()) * transMemPercentMap.getOrDefault(MatchType.REPETITIONS, 0L) / 100.0;
        double netContextMatch = safeVal.apply(stats.contextMatchSegments()) * transMemPercentMap.getOrDefault(MatchType.PERCENT_101, 0L) / 100.0;
        double netPerfect100 = safeVal.apply(stats.perfect100Segments()) * transMemPercentMap.getOrDefault(MatchType.PERCENT_100, 0L) / 100.0;
        double netFuzzy95 = safeVal.apply(stats.fuzzy95Segments()) * transMemPercentMap.getOrDefault(MatchType.PERCENT_95, 0L) / 100.0;
        double netFuzzy85 = safeVal.apply(stats.fuzzy85Segments()) * transMemPercentMap.getOrDefault(MatchType.PERCENT_85, 0L) / 100.0;
        double netFuzzy75 = safeVal.apply(stats.fuzzy75Segments()) * transMemPercentMap.getOrDefault(MatchType.PERCENT_75, 0L) / 100.0;
        double netFuzzy50 = safeVal.apply(stats.fuzzy50Segments()) * transMemPercentMap.getOrDefault(MatchType.PERCENT_50, 0L) / 100.0;
        double netNoMatch = safeVal.apply(stats.noMatchSegments()) * transMemPercentMap.getOrDefault(MatchType.PERCENT_0, 0L) / 100.0;

        log.info("Net rates - repetition: {}, contextMatch: {}, perfect100: {}, fuzzy95: {}, fuzzy85: {}, fuzzy75: {}, fuzzy50: {}, noMatch: {}, total: {}",
                netRepetition, netContextMatch, netPerfect100, netFuzzy95, netFuzzy85, netFuzzy75, netFuzzy50, netNoMatch,
                netRepetition + netContextMatch + netPerfect100 + netFuzzy95 + netFuzzy85 + netFuzzy75 + netFuzzy50 + netNoMatch);

        jobAnalysis.setNetRateRepetition(netRepetition);
        jobAnalysis.setNetRateContextMatch(netContextMatch);
        jobAnalysis.setNetRatePerfect100(netPerfect100);
        jobAnalysis.setNetRateFuzzy95(netFuzzy95);
        jobAnalysis.setNetRateFuzzy85(netFuzzy85);
        jobAnalysis.setNetRateFuzzy75(netFuzzy75);
        jobAnalysis.setNetRateFuzzy50(netFuzzy50);
        jobAnalysis.setNetRateNoMatch(netNoMatch);
        jobAnalysis.setNetRateTotal(netRepetition + netContextMatch + netPerfect100 + netFuzzy95 + netFuzzy85 + netFuzzy75 + netFuzzy50 + netNoMatch);

        return jobAnalysisRepository.save(jobAnalysis);
    }

    /**
     * Creates a new JobAnalysis using jobId.
     * This is a convenience method for the controller layer.
     *
     * @param jobId The ID of the job
     * @param user The user creating the analysis
     * @param languages The set of languages to analyze
     * @return The created JobAnalysisResponseDTO
     */
    @Transactional
    public JobAnalysisResponseDTO createJobAnalysisFromJobId(Long jobId, Long workflowStepId, User user, Set<String> languages) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));

        JobAnalysis jobAnalysis = createJobAnalysis(job, workflowStepId, user, languages);
        return toResponseDTO(jobAnalysis);
    }

    /**
     * Converts JobAnalysis entity to JobAnalysisResponseDTO.
     *
     * @param jobAnalysis The JobAnalysis entity
     * @return The JobAnalysisResponseDTO
     */
    public JobAnalysisResponseDTO toResponseDTO(JobAnalysis jobAnalysis) {
        return new JobAnalysisResponseDTO(
            jobAnalysis.getId(),
            jobAnalysis.getName(),
            jobAnalysis.getType(),
            jobAnalysis.getSourceLang(),
            jobAnalysis.getTargetLanguages(),
            jobAnalysis.getCreateDate(),
            jobAnalysis.getCreatedBy(),
            jobAnalysis.getNetRateRepetition(),
            jobAnalysis.getNetRateContextMatch(),
            jobAnalysis.getNetRatePerfect100(),
            jobAnalysis.getNetRateFuzzy95(),
            jobAnalysis.getNetRateFuzzy85(),
            jobAnalysis.getNetRateFuzzy75(),
            jobAnalysis.getNetRateFuzzy50(),
            jobAnalysis.getNetRateNoMatch(),
            jobAnalysis.getNetRateTotal()
        );
    }

    /**
     * Resolves macros in the analysis name template.
     * Supports: {projectName}, {sourceLang}, {targetLangs}, {userName}
     *
     * @param template The name template from AnalysisSetting
     * @param job The job context for macro replacement
     * @param workflowStepId The JobWorkflowStep ID to resolve the provider's username
     * @return The resolved name with macros replaced
     */
    private String resolveNameMacros(String template, Job job, Long workflowStepId) {
        if (template == null) {
            return "Analysis";
        }

        String resolved = template;

        // Replace {projectName} macro with actual project name
        if (job.getProject() != null && job.getProject().getName() != null) {
            resolved = resolved.replace("{projectName}", job.getProject().getName());
        }

        // Replace {sourceLang} macro with the job's source language
        if (job.getSourceLang() != null) {
            resolved = resolved.replace("{sourceLang}", job.getSourceLang());
        }

        // Replace {targetLangs} macro with the job's target languages joined by comma
        if (job.getTargetLangs() != null && !job.getTargetLangs().isEmpty()) {
            resolved = resolved.replace("{targetLangs}", String.join(", ", job.getTargetLangs()));
        }

        // Replace {userName} macro with the provider's username from the matching workflow step
        if (workflowStepId != null && job.getWorkflowSteps() != null) {
            String userName = job.getWorkflowSteps().stream()
                    .filter(ws -> ws.getId().equals(workflowStepId))
                    .findFirst()
                    .map(JobWorkflowStep::getProvider)
                    .map(User::getUsername)
                    .orElse(null);
            if (userName != null) {
                resolved = resolved.replace("{userName}", userName);
            }
        }

        return resolved;
    }
}
