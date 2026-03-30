package com.tms.backend.jobAnalysis;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
import com.tms.backend.projectTmAssignment.ProjectTmAssignment;
import com.tms.backend.projectTmAssignment.ProjectTmAssignmentRepository;
import com.tms.backend.settingAnalysis.AnalysisSetting;
import com.tms.backend.settingAnalysis.AnalysisSettingService;
import com.tms.backend.tomato.SizingService;
import com.tms.backend.user.User;
import com.tms.backend.workflowSteps.WorkflowStep;

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
    private final ProjectTmAssignmentRepository tmAssignmentRepo;

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
    public JobAnalysis createJobAnalysis(List<Job> jobs, Long workflowStepId, User user, TomatoSizingResponse sizingResponse) {
        Job job = jobs.get(0);

        // Get the user's analysis setting (or global default)
        AnalysisSetting setting = analysisSettingService.getUserSetting(user);

        // Create new JobAnalysis
        JobAnalysis jobAnalysis = new JobAnalysis();

        // Replace macros in the name template
        String resolvedName = resolveNameMacros(setting.getName(), job, workflowStepId);
        jobAnalysis.setName(resolvedName);

        jobAnalysis.setProject(job.getProject());
        jobAnalysis.setTargetLanguages(new java.util.HashSet<>(job.getTargetLangs()));
        jobAnalysis.setCreateDate(LocalDateTime.now());
        jobAnalysis.setCreatedBy(user.getUsername());
        jobAnalysis.setType(JobAnalysisType.DEFAULT);

        TomatoSizingResponse.Statistics stats = sizingResponse.statistics();

        log.info("Sizing stats - repetition: {}, contextMatch: {}, perfect100: {}, fuzzy95: {}, fuzzy85: {}, fuzzy75: {}, fuzzy50: {}, noMatch: {}",
                stats.repetitionSegments(), stats.contextMatchSegments(), stats.perfect100Segments(),
                stats.fuzzy95Segments(), stats.fuzzy85Segments(), stats.fuzzy75Segments(),
                stats.fuzzy50Segments(), stats.noMatchSegments());

        // Set source and target language from API response
        jobAnalysis.setSourceLang(stats.sourceLanguage());
        if (stats.targetLanguage() != null) {
            jobAnalysis.setTargetLanguages(java.util.Set.of(stats.targetLanguage()));
        }

        // Read TM words, characters, and segments from API response
        jobAnalysis.setRepetitionWords(safeLong(stats.repetitionTM_Words()));
        jobAnalysis.setRepetitionCharacters(safeLong(stats.repetitionTM_Characters()));
        jobAnalysis.setRepetitionSegments(safeLong(stats.repetitionTM_Segments()));
        jobAnalysis.setContextMatchWords(safeLong(stats.context101TM_Words()));
        jobAnalysis.setContextMatchCharacters(safeLong(stats.context101TM_Characters()));
        jobAnalysis.setContextMatchSegments(safeLong(stats.context101TM_Segments()));
        jobAnalysis.setPerfect100Words(safeLong(stats.perfect100TM_Words()));
        jobAnalysis.setPerfect100Characters(safeLong(stats.perfect100TM_Characters()));
        jobAnalysis.setPerfect100Segments(safeLong(stats.perfect100TM_Segments()));
        jobAnalysis.setFuzzy95Words(safeLong(stats.fuzzy95TM_Words()));
        jobAnalysis.setFuzzy95Characters(safeLong(stats.fuzzy95TM_Characters()));
        jobAnalysis.setFuzzy95Segments(safeLong(stats.fuzzy95TM_Segments()));
        jobAnalysis.setFuzzy85Words(safeLong(stats.fuzzy85TM_Words()));
        jobAnalysis.setFuzzy85Characters(safeLong(stats.fuzzy85TM_Characters()));
        jobAnalysis.setFuzzy85Segments(safeLong(stats.fuzzy85TM_Segments()));
        jobAnalysis.setFuzzy75Words(safeLong(stats.fuzzy75TM_Words()));
        jobAnalysis.setFuzzy75Characters(safeLong(stats.fuzzy75TM_Characters()));
        jobAnalysis.setFuzzy75Segments(safeLong(stats.fuzzy75TM_Segments()));
        jobAnalysis.setFuzzy50Words(safeLong(stats.fuzzy50TM_Words()));
        jobAnalysis.setFuzzy50Characters(safeLong(stats.fuzzy50TM_Characters()));
        jobAnalysis.setFuzzy50Segments(safeLong(stats.fuzzy50TM_Segments()));
        jobAnalysis.setNoMatchWords(safeLong(stats.noMatchTM_Words()));
        jobAnalysis.setNoMatchCharacters(safeLong(stats.noMatchTM_Characters()));
        jobAnalysis.setNoMatchSegments(safeLong(stats.noMatchTM_Segments()));

        log.info("TM Words - repetition: {}, contextMatch: {}, perfect100: {}, fuzzy95: {}, fuzzy85: {}, fuzzy75: {}, fuzzy50: {}, noMatch: {}",
                jobAnalysis.getRepetitionWords(), jobAnalysis.getContextMatchWords(), jobAnalysis.getPerfect100Words(),
                jobAnalysis.getFuzzy95Words(), jobAnalysis.getFuzzy85Words(), jobAnalysis.getFuzzy75Words(),
                jobAnalysis.getFuzzy50Words(), jobAnalysis.getNoMatchWords());

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
    public JobAnalysisResponseDTO createJobAnalysisFromJobIds(List<Long> jobIds, Long workflowStepId, User user) {
        List<Job> jobs = jobIds.stream()
                .map(id -> jobRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Job not found with id: " + id)))
                .collect(Collectors.toList());

        Job primaryJob = jobs.get(0);

        // Resolve the actual WorkflowStep from the JobWorkflowStep id
        JobWorkflowStep jobWorkflowStep = jobWorkflowStepRepository.findById(workflowStepId)
                .orElseThrow(() -> new RuntimeException("JobWorkflowStep not found with id: " + workflowStepId));
        WorkflowStep workflowStep = jobWorkflowStep.getWorkflowStep();
        Long actualWorkflowStepId = workflowStep.getId();
        log.info("Resolved JobWorkflowStep id {} -> WorkflowStep id {} (name: {})",
                workflowStepId, actualWorkflowStepId, workflowStep.getName());

        // Get default net rate scheme and find the matching workflow step rates
        NetRateSchemeResponseDTO defaultScheme = netRateSchemeService.getDefaultScheme();
        WorkflowStepRateResponseDTO stepRate = defaultScheme.workflowStepRates().stream()
                .filter(wf -> wf.workflowStepId().equals(actualWorkflowStepId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No rates found for workflowStepId: " + actualWorkflowStepId));

        boolean isMTPE = isMTPEWorkflowStep(workflowStep);
        log.info("Workflow step '{}' identified as: {}", workflowStep.getName(), isMTPE ? "MTPE" : "Translation");

        Map<MatchType, Long> netRatePercentMap = stepRate.matchTypeRates().stream()
                .collect(Collectors.toMap(
                        MatchTypeRateResponseDTO::matchType,
                        rate -> isMTPE ? rate.machineTransPercent() : rate.transMemoryPercent()
                ));
        log.info("netRatePercentMap for workflowStepId {}: {}", actualWorkflowStepId, netRatePercentMap);

        // Resolve the TM with read access for this project + workflow step
        Long projectId = primaryJob.getProject().getId();
        Long tmId = tmAssignmentRepo
                .findByProjectIdAndWorkflowStepIdAndReadAccessTrue(projectId, actualWorkflowStepId)
                .map(ProjectTmAssignment::getTmId)
                .orElseThrow(() -> new RuntimeException(
                        "No translation memory with read access assigned for project "
                                + projectId + " and workflow step " + actualWorkflowStepId));
        log.info("Resolved tmId {} for project {} and workflowStep {} ({})",
                tmId, projectId, actualWorkflowStepId, workflowStep.getName());

        // Send files to Tomato API
        List<String> filePaths = jobs.stream()
                .map(Job::getOriginalFilePath)
                .collect(Collectors.toList());
        TomatoSizingResponse sizingResponse = sizingService.sendFilesToTomatoAPIByPath(filePaths, netRatePercentMap, tmId);

        JobAnalysis jobAnalysis = createJobAnalysis(jobs, workflowStepId, user, sizingResponse);
        JobAnalysisResponseDTO dto = JobAnalysisResponseDTO.fromEntity(jobAnalysis, sizingResponse);
        log.info("JobAnalysisResponseDTO: {}", dto);
        return dto;
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

    private boolean isMTPEWorkflowStep(WorkflowStep workflowStep) {
        String name = workflowStep.getName();
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.contains("mtpe") || lower.contains("post-editing") || lower.contains("post editing");
    }

    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }
}
