package com.tms.backend.jobAnalysis;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tms.backend.dto.JobAnalysisResponseDTO;
import com.tms.backend.dto.MatchTypeRateResponseDTO;
import com.tms.backend.dto.NetRateSchemeResponseDTO;
import com.tms.backend.dto.TomatoSizingResponse;
import com.tms.backend.dto.WorkflowStepRateResponseDTO;
import com.tms.backend.job.Job;
import com.tms.backend.job.JobRepository;
import com.tms.backend.job.JobWorkflowStep;
import com.tms.backend.job.JobWorkflowStepRepository;
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
    private final JobAnalysisFileRepository jobAnalysisFileRepository;
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

        jobAnalysis.setApprovedTM_Weighted(stats.approvedTM_Weighted());
        jobAnalysis.setApprovedNT_Weighted(stats.approvedNT_Weighted());
        jobAnalysis.setRepetitionTM_Weighted(stats.repetitionTM_Weighted());
        jobAnalysis.setRepetitionNT_Weighted(stats.repetitionNT_Weighted());
        jobAnalysis.setContext101TM_Weighted(stats.context101TM_Weighted());
        jobAnalysis.setContext101NT_Weighted(stats.context101NT_Weighted());
        jobAnalysis.setPerfect100TM_Weighted(stats.perfect100TM_Weighted());
        jobAnalysis.setPerfect100NT_Weighted(stats.perfect100NT_Weighted());
        jobAnalysis.setFuzzy95TM_Weighted(stats.fuzzy95TM_Weighted());
        jobAnalysis.setFuzzy95NT_Weighted(stats.fuzzy95NT_Weighted());
        jobAnalysis.setFuzzy85TM_Weighted(stats.fuzzy85TM_Weighted());
        jobAnalysis.setFuzzy85NT_Weighted(stats.fuzzy85NT_Weighted());
        jobAnalysis.setFuzzy75TM_Weighted(stats.fuzzy75TM_Weighted());
        jobAnalysis.setFuzzy75NT_Weighted(stats.fuzzy75NT_Weighted());
        jobAnalysis.setFuzzy50TM_Weighted(stats.fuzzy50TM_Weighted());
        jobAnalysis.setFuzzy50NT_Weighted(stats.fuzzy50NT_Weighted());
        jobAnalysis.setNoMatchTM_Weighted(stats.noMatchTM_Weighted());
        jobAnalysis.setNoMatchNT_Weighted(stats.noMatchNT_Weighted());
        jobAnalysis.setTotalWeighted(stats.totalWeighted());
        jobAnalysis.setTotalWeightedPercentage(stats.totalWeightedPercentage());

        log.info("TM Words - repetition: {}, contextMatch: {}, perfect100: {}, fuzzy95: {}, fuzzy85: {}, fuzzy75: {}, fuzzy50: {}, noMatch: {}",
                jobAnalysis.getRepetitionWords(), jobAnalysis.getContextMatchWords(), jobAnalysis.getPerfect100Words(),
                jobAnalysis.getFuzzy95Words(), jobAnalysis.getFuzzy85Words(), jobAnalysis.getFuzzy75Words(),
                jobAnalysis.getFuzzy50Words(), jobAnalysis.getNoMatchWords());

        JobAnalysis saved = jobAnalysisRepository.save(jobAnalysis);

        if (sizingResponse.files() != null) {
            List<JobAnalysisFile> fileEntities = sizingResponse.files().stream().map(f -> {
                TomatoSizingResponse.Statistics s = f.statistics();
                JobAnalysisFile file = new JobAnalysisFile();
                file.setJobAnalysis(saved);
                file.setFileName(f.fileName());
                file.setApprovedTM_Words(safeLong(s.approvedTM_Words()));
                file.setApprovedTM_Characters(safeLong(s.approvedTM_Characters()));
                file.setApprovedTM_Segments(safeLong(s.approvedTM_Segments()));
                file.setRepetitionTM_Words(safeLong(s.repetitionTM_Words()));
                file.setRepetitionTM_Characters(safeLong(s.repetitionTM_Characters()));
                file.setRepetitionTM_Segments(safeLong(s.repetitionTM_Segments()));
                file.setContext101TM_Words(safeLong(s.context101TM_Words()));
                file.setContext101TM_Characters(safeLong(s.context101TM_Characters()));
                file.setContext101TM_Segments(safeLong(s.context101TM_Segments()));
                file.setPerfect100TM_Words(safeLong(s.perfect100TM_Words()));
                file.setPerfect100TM_Characters(safeLong(s.perfect100TM_Characters()));
                file.setPerfect100TM_Segments(safeLong(s.perfect100TM_Segments()));
                file.setFuzzy95TM_Words(safeLong(s.fuzzy95TM_Words()));
                file.setFuzzy95TM_Characters(safeLong(s.fuzzy95TM_Characters()));
                file.setFuzzy95TM_Segments(safeLong(s.fuzzy95TM_Segments()));
                file.setFuzzy85TM_Words(safeLong(s.fuzzy85TM_Words()));
                file.setFuzzy85TM_Characters(safeLong(s.fuzzy85TM_Characters()));
                file.setFuzzy85TM_Segments(safeLong(s.fuzzy85TM_Segments()));
                file.setFuzzy75TM_Words(safeLong(s.fuzzy75TM_Words()));
                file.setFuzzy75TM_Characters(safeLong(s.fuzzy75TM_Characters()));
                file.setFuzzy75TM_Segments(safeLong(s.fuzzy75TM_Segments()));
                file.setFuzzy50TM_Words(safeLong(s.fuzzy50TM_Words()));
                file.setFuzzy50TM_Characters(safeLong(s.fuzzy50TM_Characters()));
                file.setFuzzy50TM_Segments(safeLong(s.fuzzy50TM_Segments()));
                file.setNoMatchTM_Words(safeLong(s.noMatchTM_Words()));
                file.setNoMatchTM_Characters(safeLong(s.noMatchTM_Characters()));
                file.setNoMatchTM_Segments(safeLong(s.noMatchTM_Segments()));
                file.setApprovedTM_Weighted(s.approvedTM_Weighted());
                file.setApprovedNT_Weighted(s.approvedNT_Weighted());
                file.setRepetitionTM_Weighted(s.repetitionTM_Weighted());
                file.setRepetitionNT_Weighted(s.repetitionNT_Weighted());
                file.setContext101TM_Weighted(s.context101TM_Weighted());
                file.setContext101NT_Weighted(s.context101NT_Weighted());
                file.setPerfect100TM_Weighted(s.perfect100TM_Weighted());
                file.setPerfect100NT_Weighted(s.perfect100NT_Weighted());
                file.setFuzzy95TM_Weighted(s.fuzzy95TM_Weighted());
                file.setFuzzy95NT_Weighted(s.fuzzy95NT_Weighted());
                file.setFuzzy85TM_Weighted(s.fuzzy85TM_Weighted());
                file.setFuzzy85NT_Weighted(s.fuzzy85NT_Weighted());
                file.setFuzzy75TM_Weighted(s.fuzzy75TM_Weighted());
                file.setFuzzy75NT_Weighted(s.fuzzy75NT_Weighted());
                file.setFuzzy50TM_Weighted(s.fuzzy50TM_Weighted());
                file.setFuzzy50NT_Weighted(s.fuzzy50NT_Weighted());
                file.setNoMatchTM_Weighted(s.noMatchTM_Weighted());
                file.setNoMatchNT_Weighted(s.noMatchNT_Weighted());
                file.setTotalWeighted(s.totalWeighted());
                file.setTotalWeightedPercentage(s.totalWeightedPercentage());
                return file;
            }).collect(Collectors.toList());
            jobAnalysisFileRepository.saveAll(fileEntities);
            saved.setFiles(fileEntities);
        }

        return saved;
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

        log.info("matchTypeRates for workflowStepId {}: {}", actualWorkflowStepId, stepRate.matchTypeRates());

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

        // Build sizingRequestJson with the full net rate scheme structure Tomato expects
        String sizingRequestJson = buildSizingRequestJson(defaultScheme, projectId, stepRate.matchTypeRates());
        log.info("sizingRequestJson: {}", sizingRequestJson);

        // Send files to Tomato API
        List<String> filePaths = jobs.stream()
                .map(Job::getOriginalFilePath)
                .collect(Collectors.toList());
        TomatoSizingResponse sizingResponse = sizingService.sendFilesToTomatoAPIByPath(filePaths, sizingRequestJson, tmId);

        JobAnalysis jobAnalysis = createJobAnalysis(jobs, workflowStepId, user, sizingResponse);
        JobAnalysisResponseDTO dto = JobAnalysisResponseDTO.fromEntity(jobAnalysis);
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
    @Transactional(readOnly = true)
    public List<JobAnalysisResponseDTO> getAllJobAnalyses() {
        return jobAnalysisRepository.findAll().stream()
                .map(JobAnalysisResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<JobAnalysisResponseDTO> getJobAnalysesByProjectId(Long projectId) {
        return jobAnalysisRepository.findByProjectId(projectId).stream()
                .map(JobAnalysisResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public JobAnalysisResponseDTO getJobAnalysis(Long id) {
        JobAnalysis jobAnalysis = jobAnalysisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("JobAnalysis not found with id: " + id));
        return JobAnalysisResponseDTO.fromEntity(jobAnalysis);
    }

    @Transactional
    public void deleteJobAnalysis(Long id) {
        if (!jobAnalysisRepository.existsById(id)) {
            throw new RuntimeException("JobAnalysis not found with id: " + id);
        }
        jobAnalysisRepository.deleteById(id);
    }

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

    private String buildSizingRequestJson(NetRateSchemeResponseDTO scheme, Long projectId, List<MatchTypeRateResponseDTO> rates) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();
            root.put("id", scheme.id());
            root.put("name", scheme.name());
            root.put("projectId", projectId);
            ArrayNode matchTypeRates = root.putArray("matchTypeRates");
            for (MatchTypeRateResponseDTO rate : rates) {
                ObjectNode rateNode = matchTypeRates.addObject();
                rateNode.put("matchType", rate.matchType().name());
                rateNode.put("transMemoryPercent", rate.transMemoryPercent());
                rateNode.put("nonTranslatablePercent", rate.nonTranslatablePercent());
                rateNode.put("machineTransPercent", rate.machineTransPercent());
            }
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build sizingRequestJson", e);
        }
    }

    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }
}
