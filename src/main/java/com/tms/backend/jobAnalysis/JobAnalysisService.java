package com.tms.backend.jobAnalysis;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
import com.tms.backend.dto.SizingStatusDTO;
import com.tms.backend.dto.TomatoSizingResponse;
import com.tms.backend.job.Job;
import com.tms.backend.job.JobRepository;
import com.tms.backend.netRateScheme.NetRateScheme;
import com.tms.backend.netRateScheme.NetRateSchemeService;
import com.tms.backend.project.Project;
import com.tms.backend.projectTmAssignment.ProjectTmAssignment;
import com.tms.backend.projectTmAssignment.ProjectTmAssignmentRepository;
import com.tms.backend.settingAnalysis.AnalysisSetting;
import com.tms.backend.settingAnalysis.AnalysisSettingService;
import com.tms.backend.tomato.SizingPollService;
import com.tms.backend.tomato.SizingPollStatus;
import com.tms.backend.tomato.SizingService;
import com.tms.backend.user.User;

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
    private final SizingPollService sizingPollService;
    private final NetRateSchemeService netRateSchemeService;
    private final ProjectTmAssignmentRepository tmAssignmentRepo;
    private final PendingSizingJobRepository pendingSizingJobRepository;

    /**
     * Resolves all context needed for sizing, submits files to Tomato, and starts background polling.
     * Returns the Tomato jobId immediately so the caller can check status later.
     */
    @Transactional
    public String initiateSizing(List<Long> jobIds, User user, Boolean preTranslate, Integer minSimilarity) {
        List<Job> jobs = jobIds.stream()
                .map(id -> jobRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Job not found with id: " + id)))
                .collect(Collectors.toList());

        Job primaryJob = jobs.get(0);
        Project project = primaryJob.getProject();
        Long projectId = project.getId();

        NetRateSchemeResponseDTO scheme = resolveSchemeForProject(project);

        String sourceLanguage = primaryJob.getSourceLang();
        String targetLanguage = primaryJob.getTargetLangs() != null
                ? primaryJob.getTargetLangs().stream().findFirst().orElse(null)
                : null;

        List<Long> tmIds = tmAssignmentRepo
                .findByProjectIdAndSourceLangAndTargetLangAndReadAccessTrue(
                        projectId, sourceLanguage, targetLanguage)
                .stream()
                .map(ProjectTmAssignment::getTmId)
                .distinct()
                .collect(Collectors.toList());

        if (tmIds.isEmpty()) {
            throw new RuntimeException(
                    "No TM with read access found for project " + projectId
                            + " with language pair " + sourceLanguage + " -> " + targetLanguage);
        }

        log.info("Sizing request for project {} using tmIds: {}", projectId, tmIds);

        String sizingRequestJson = buildSizingRequestJson(scheme, projectId);

        List<String> filePaths = jobs.stream()
                .map(Job::getOriginalFilePath)
                .collect(Collectors.toList());

        Integer effectiveMinSimilarity = Boolean.TRUE.equals(preTranslate) ? minSimilarity : null;

        String tomatoJobId = sizingService.sendFilesToTomatoAPIByPath(
                filePaths, sizingRequestJson, tmIds, sourceLanguage, targetLanguage, effectiveMinSimilarity);

        pendingSizingJobRepository.save(new PendingSizingJob(tomatoJobId, jobIds, projectId, user));
        sizingPollService.startPolling(tomatoJobId);

        log.info("Sizing initiated for tomatoJobId: {}", tomatoJobId);
        return tomatoJobId;
    }

    /**
     * Called by the background polling thread when Tomato reports completion.
     * Creates and persists the JobAnalysis using the stored context.
     */
    @Transactional
    public void finalizeJobAnalysis(String tomatoJobId, TomatoSizingResponse sizingResponse) {
        PendingSizingJob ctx = pendingSizingJobRepository.findById(tomatoJobId).orElse(null);
        if (ctx == null) {
            log.warn("finalizeJobAnalysis called but no context found for jobId: {}", tomatoJobId);
            return;
        }

        List<Job> jobs = ctx.getJobIds().stream()
                .map(id -> jobRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Job not found with id: " + id)))
                .collect(Collectors.toList());

        createJobAnalysis(jobs, ctx.getUser(), sizingResponse, tomatoJobId);
        pendingSizingJobRepository.delete(ctx);
        log.info("JobAnalysis created and saved for tomatoJobId: {}", tomatoJobId);
    }

    /**
     * Checks the current sizing status by calling Tomato directly.
     * When completed, creates and persists the JobAnalysis.
     */
    @Transactional
    public SizingStatusDTO getSizingStatus(String tomatoJobId) {
        PendingSizingJob ctx = pendingSizingJobRepository.findById(tomatoJobId)
                .orElseThrow(() -> new RuntimeException("No pending sizing job found for: " + tomatoJobId));

        SizingPollStatus pollStatus = sizingService.fetchSizingResultOnce(tomatoJobId);

        if (!pollStatus.isCompleted()) {
            return new SizingStatusDTO("pending", pollStatus.progressPercent(), null);
        }

        List<Job> jobs = ctx.getJobIds().stream()
                .map(id -> jobRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Job not found with id: " + id)))
                .collect(Collectors.toList());

        JobAnalysis jobAnalysis = createJobAnalysis(jobs, ctx.getUser(), pollStatus.result(), tomatoJobId);
        pendingSizingJobRepository.delete(ctx);

        return new SizingStatusDTO("completed", 100.0, JobAnalysisResponseDTO.fromEntity(jobAnalysis));
    }

    @Transactional
    public JobAnalysis createJobAnalysis(List<Job> jobs, User user, TomatoSizingResponse sizingResponse, String tomatoJobId) {
        Job job = jobs.get(0);

        AnalysisSetting setting = analysisSettingService.getUserSetting(user);

        JobAnalysis jobAnalysis = new JobAnalysis();

        String resolvedName = resolveNameMacros(setting.getName(), job);
        jobAnalysis.setName(resolvedName);
        jobAnalysis.setTomatoJobId(tomatoJobId);

        jobAnalysis.setProject(job.getProject());
        jobAnalysis.setTargetLanguages(new java.util.HashSet<>(job.getTargetLangs()));
        jobAnalysis.setCreateDate(LocalDateTime.now());
        jobAnalysis.setCreatedBy(user.getUsername());
        jobAnalysis.setType(JobAnalysisType.DEFAULT);

        TomatoSizingResponse.Statistics stats = sizingResponse.statistics();

        try {
            String prettyStats = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(stats);
            log.info("Raw TomatoSizingResponse.statistics:\n{}", prettyStats);
        } catch (Exception e) {
            log.warn("Failed to pretty-print TomatoSizingResponse.statistics", e);
        }

        jobAnalysis.setSourceLang(stats.sourceLanguage());
        if (stats.targetLanguage() != null) {
            jobAnalysis.setTargetLanguages(java.util.Set.of(stats.targetLanguage()));
        }

        jobAnalysis.setApprovedSegments(safeLong(stats.approvedSegments()));
        jobAnalysis.setApprovedWords(safeLong(stats.approvedWords()));
        jobAnalysis.setApprovedCharacters(safeLong(stats.approvedCharacters()));
        jobAnalysis.setApprovedWeighted(stats.approvedWeighted());
        jobAnalysis.setApprovedPercentage(stats.approvedPercentage());

        jobAnalysis.setRepetitionSegments(safeLong(stats.repetitionSegments()));
        jobAnalysis.setRepetitionWords(safeLong(stats.repetitionWords()));
        jobAnalysis.setRepetitionCharacters(safeLong(stats.repetitionCharacters()));
        jobAnalysis.setRepetitionWeighted(stats.repetitionWeighted());
        jobAnalysis.setRepetitionPercentage(stats.repetitionPercentage());

        jobAnalysis.setContext101Segments(safeLong(stats.context101Segments()));
        jobAnalysis.setContext101Words(safeLong(stats.context101Words()));
        jobAnalysis.setContext101Characters(safeLong(stats.context101Characters()));
        jobAnalysis.setContext101Weighted(stats.context101Weighted());
        jobAnalysis.setContext101Percentage(stats.context101Percentage());

        jobAnalysis.setPerfect100Segments(safeLong(stats.perfect100Segments()));
        jobAnalysis.setPerfect100Words(safeLong(stats.perfect100Words()));
        jobAnalysis.setPerfect100Characters(safeLong(stats.perfect100Characters()));
        jobAnalysis.setPerfect100Weighted(stats.perfect100Weighted());
        jobAnalysis.setPerfect100Percentage(stats.perfect100Percentage());

        jobAnalysis.setFuzzy95Segments(safeLong(stats.fuzzy95Segments()));
        jobAnalysis.setFuzzy95Words(safeLong(stats.fuzzy95Words()));
        jobAnalysis.setFuzzy95Characters(safeLong(stats.fuzzy95Characters()));
        jobAnalysis.setFuzzy95Weighted(stats.fuzzy95Weighted());
        jobAnalysis.setFuzzy95Percentage(stats.fuzzy95Percentage());

        jobAnalysis.setFuzzy85Segments(safeLong(stats.fuzzy85Segments()));
        jobAnalysis.setFuzzy85Words(safeLong(stats.fuzzy85Words()));
        jobAnalysis.setFuzzy85Characters(safeLong(stats.fuzzy85Characters()));
        jobAnalysis.setFuzzy85Weighted(stats.fuzzy85Weighted());
        jobAnalysis.setFuzzy85Percentage(stats.fuzzy85Percentage());

        jobAnalysis.setFuzzy75Segments(safeLong(stats.fuzzy75Segments()));
        jobAnalysis.setFuzzy75Words(safeLong(stats.fuzzy75Words()));
        jobAnalysis.setFuzzy75Characters(safeLong(stats.fuzzy75Characters()));
        jobAnalysis.setFuzzy75Weighted(stats.fuzzy75Weighted());
        jobAnalysis.setFuzzy75Percentage(stats.fuzzy75Percentage());

        jobAnalysis.setFuzzy50Segments(safeLong(stats.fuzzy50Segments()));
        jobAnalysis.setFuzzy50Words(safeLong(stats.fuzzy50Words()));
        jobAnalysis.setFuzzy50Characters(safeLong(stats.fuzzy50Characters()));
        jobAnalysis.setFuzzy50Weighted(stats.fuzzy50Weighted());
        jobAnalysis.setFuzzy50Percentage(stats.fuzzy50Percentage());

        jobAnalysis.setNoMatchSegments(safeLong(stats.noMatchSegments()));
        jobAnalysis.setNoMatchWords(safeLong(stats.noMatchWords()));
        jobAnalysis.setNoMatchCharacters(safeLong(stats.noMatchCharacters()));
        jobAnalysis.setNoMatchWeighted(stats.noMatchWeighted());
        jobAnalysis.setNoMatchPercentage(stats.noMatchPercentage());

        jobAnalysis.setTotalSegments(stats.totalSegments());
        jobAnalysis.setTotalWords(stats.totalWords());
        jobAnalysis.setTotalCharacters(stats.totalCharacters());
        jobAnalysis.setTotalWeighted(stats.totalWeighted());
        jobAnalysis.setTotalWeightedPercentage(stats.totalWeightedPercentage());
        jobAnalysis.setUnitType(stats.unitType());
        jobAnalysis.setTmNames(stats.tmNames());

        log.info("Sizing mapping check [words]\n"
                + "  repetition   raw={} -> mapped={}\n"
                + "  context101   raw={} -> mapped={}\n"
                + "  perfect100   raw={} -> mapped={}\n"
                + "  fuzzy95      raw={} -> mapped={}\n"
                + "  fuzzy85      raw={} -> mapped={}\n"
                + "  fuzzy75      raw={} -> mapped={}\n"
                + "  fuzzy50      raw={} -> mapped={}\n"
                + "  noMatch      raw={} -> mapped={}\n"
                + "  totalWords   raw={} -> mapped={}",
                stats.repetitionWords(), jobAnalysis.getRepetitionWords(),
                stats.context101Words(), jobAnalysis.getContext101Words(),
                stats.perfect100Words(), jobAnalysis.getPerfect100Words(),
                stats.fuzzy95Words(), jobAnalysis.getFuzzy95Words(),
                stats.fuzzy85Words(), jobAnalysis.getFuzzy85Words(),
                stats.fuzzy75Words(), jobAnalysis.getFuzzy75Words(),
                stats.fuzzy50Words(), jobAnalysis.getFuzzy50Words(),
                stats.noMatchWords(), jobAnalysis.getNoMatchWords(),
                stats.totalWords(), jobAnalysis.getTotalWords());

        JobAnalysis saved = jobAnalysisRepository.save(jobAnalysis);

        List<TomatoSizingResponse> fileList = sizingResponse.files() != null
                ? sizingResponse.files()
                : (sizingResponse.fileName() != null ? List.of(sizingResponse) : List.of());

        if (!fileList.isEmpty()) {
            List<JobAnalysisFile> fileEntities = fileList.stream().map(f -> {
                TomatoSizingResponse.Statistics s = f.statistics();
                JobAnalysisFile file = new JobAnalysisFile();
                file.setJobAnalysis(saved);
                file.setFileName(f.fileName());

                file.setApprovedSegments(safeLong(s.approvedSegments()));
                file.setApprovedWords(safeLong(s.approvedWords()));
                file.setApprovedCharacters(safeLong(s.approvedCharacters()));
                file.setApprovedWeighted(s.approvedWeighted());
                file.setApprovedPercentage(s.approvedPercentage());

                file.setRepetitionSegments(safeLong(s.repetitionSegments()));
                file.setRepetitionWords(safeLong(s.repetitionWords()));
                file.setRepetitionCharacters(safeLong(s.repetitionCharacters()));
                file.setRepetitionWeighted(s.repetitionWeighted());
                file.setRepetitionPercentage(s.repetitionPercentage());

                file.setContext101Segments(safeLong(s.context101Segments()));
                file.setContext101Words(safeLong(s.context101Words()));
                file.setContext101Characters(safeLong(s.context101Characters()));
                file.setContext101Weighted(s.context101Weighted());
                file.setContext101Percentage(s.context101Percentage());

                file.setPerfect100Segments(safeLong(s.perfect100Segments()));
                file.setPerfect100Words(safeLong(s.perfect100Words()));
                file.setPerfect100Characters(safeLong(s.perfect100Characters()));
                file.setPerfect100Weighted(s.perfect100Weighted());
                file.setPerfect100Percentage(s.perfect100Percentage());

                file.setFuzzy95Segments(safeLong(s.fuzzy95Segments()));
                file.setFuzzy95Words(safeLong(s.fuzzy95Words()));
                file.setFuzzy95Characters(safeLong(s.fuzzy95Characters()));
                file.setFuzzy95Weighted(s.fuzzy95Weighted());
                file.setFuzzy95Percentage(s.fuzzy95Percentage());

                file.setFuzzy85Segments(safeLong(s.fuzzy85Segments()));
                file.setFuzzy85Words(safeLong(s.fuzzy85Words()));
                file.setFuzzy85Characters(safeLong(s.fuzzy85Characters()));
                file.setFuzzy85Weighted(s.fuzzy85Weighted());
                file.setFuzzy85Percentage(s.fuzzy85Percentage());

                file.setFuzzy75Segments(safeLong(s.fuzzy75Segments()));
                file.setFuzzy75Words(safeLong(s.fuzzy75Words()));
                file.setFuzzy75Characters(safeLong(s.fuzzy75Characters()));
                file.setFuzzy75Weighted(s.fuzzy75Weighted());
                file.setFuzzy75Percentage(s.fuzzy75Percentage());

                file.setFuzzy50Segments(safeLong(s.fuzzy50Segments()));
                file.setFuzzy50Words(safeLong(s.fuzzy50Words()));
                file.setFuzzy50Characters(safeLong(s.fuzzy50Characters()));
                file.setFuzzy50Weighted(s.fuzzy50Weighted());
                file.setFuzzy50Percentage(s.fuzzy50Percentage());

                file.setNoMatchSegments(safeLong(s.noMatchSegments()));
                file.setNoMatchWords(safeLong(s.noMatchWords()));
                file.setNoMatchCharacters(safeLong(s.noMatchCharacters()));
                file.setNoMatchWeighted(s.noMatchWeighted());
                file.setNoMatchPercentage(s.noMatchPercentage());

                file.setTotalSegments(s.totalSegments());
                file.setTotalWords(s.totalWords());
                file.setTotalCharacters(s.totalCharacters());
                file.setTotalWeighted(s.totalWeighted());
                file.setTotalWeightedPercentage(s.totalWeightedPercentage());
                return file;
            }).collect(Collectors.toList());
            jobAnalysisFileRepository.saveAll(fileEntities);
            saved.setFiles(fileEntities);
        }

        return saved;
    }

    @Transactional
    public List<JobAnalysisResponseDTO> getAllJobAnalyses() {
        List<PendingSizingJob> stillPending = new ArrayList<>();

        for (PendingSizingJob ctx : pendingSizingJobRepository.findAll()) {
            try {
                SizingPollStatus pollStatus = sizingService.fetchSizingResultOnce(ctx.getTomatoJobId());
                if (pollStatus.isCompleted()) {
                    List<Job> jobs = ctx.getJobIds().stream()
                            .map(id -> jobRepository.findById(id)
                                    .orElseThrow(() -> new RuntimeException("Job not found with id: " + id)))
                            .collect(Collectors.toList());
                    createJobAnalysis(jobs, ctx.getUser(), pollStatus.result(), ctx.getTomatoJobId());
                    pendingSizingJobRepository.delete(ctx);
                    log.info("Resolved pending sizing job {} during getAllJobAnalyses", ctx.getTomatoJobId());
                } else {
                    stillPending.add(ctx);
                }
            } catch (Exception e) {
                log.warn("Could not resolve pending sizing job {}: {}", ctx.getTomatoJobId(), e.getMessage());
                stillPending.add(ctx);
            }
        }

        List<JobAnalysisResponseDTO> result = new ArrayList<>(
                jobAnalysisRepository.findAll().stream()
                        .map(JobAnalysisResponseDTO::fromEntity)
                        .collect(Collectors.toList())
        );

        stillPending.stream()
                .map(ctx -> JobAnalysisResponseDTO.pending(ctx.getTomatoJobId()))
                .forEach(result::add);

        return result;
    }

    @Transactional
    public List<JobAnalysisResponseDTO> getJobAnalysesByProjectId(Long projectId) {
        List<PendingSizingJob> stillPending = new ArrayList<>();

        for (PendingSizingJob ctx : pendingSizingJobRepository.findByProjectId(projectId)) {
            try {
                SizingPollStatus pollStatus = sizingService.fetchSizingResultOnce(ctx.getTomatoJobId());
                if (pollStatus.isCompleted()) {
                    List<Job> jobs = ctx.getJobIds().stream()
                            .map(id -> jobRepository.findById(id)
                                    .orElseThrow(() -> new RuntimeException("Job not found with id: " + id)))
                            .collect(Collectors.toList());
                    createJobAnalysis(jobs, ctx.getUser(), pollStatus.result(), ctx.getTomatoJobId());
                    pendingSizingJobRepository.delete(ctx);
                    log.info("Resolved pending sizing job {} during getJobAnalysesByProjectId", ctx.getTomatoJobId());
                } else {
                    stillPending.add(ctx);
                }
            } catch (Exception e) {
                log.warn("Could not resolve pending sizing job {}: {}", ctx.getTomatoJobId(), e.getMessage());
                stillPending.add(ctx);
            }
        }

        List<JobAnalysisResponseDTO> result = new ArrayList<>(
                jobAnalysisRepository.findByProjectId(projectId).stream()
                        .map(JobAnalysisResponseDTO::fromEntity)
                        .collect(Collectors.toList())
        );

        stillPending.stream()
                .map(ctx -> JobAnalysisResponseDTO.pending(ctx.getTomatoJobId()))
                .forEach(result::add);

        return result;
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

    private NetRateSchemeResponseDTO resolveSchemeForProject(Project project) {
        // 1. Project's own scheme
        NetRateScheme projectScheme = project.getNetRateScheme();
        if (projectScheme != null) {
            NetRateSchemeResponseDTO dto = netRateSchemeService.toDTO(projectScheme);
            if (!dto.matchTypeRates().isEmpty()) return dto;
            log.warn("Project scheme '{}' (id={}) has no match type rates, falling back to default", dto.name(), dto.id());
        }
        // 2. Client's scheme
        if (project.getClient() != null) {
            NetRateScheme clientScheme = project.getClient().getNetRateScheme();
            if (clientScheme != null) {
                NetRateSchemeResponseDTO dto = netRateSchemeService.toDTO(clientScheme);
                if (!dto.matchTypeRates().isEmpty()) return dto;
                log.warn("Client scheme '{}' (id={}) has no match type rates, falling back to default", dto.name(), dto.id());
            }
        }
        // 3. Global default
        return netRateSchemeService.getDefaultScheme();
    }

    private String resolveNameMacros(String template, Job job) {
        if (template == null) {
            return "Analysis";
        }

        String resolved = template;

        if (job.getProject() != null && job.getProject().getName() != null) {
            resolved = resolved.replace("{projectName}", job.getProject().getName());
        }

        if (job.getSourceLang() != null) {
            resolved = resolved.replace("{sourceLang}", job.getSourceLang());
        }

        if (job.getTargetLangs() != null && !job.getTargetLangs().isEmpty()) {
            resolved = resolved.replace("{targetLangs}", String.join(", ", job.getTargetLangs()));
        }

        return resolved;
    }

    private String buildSizingRequestJson(NetRateSchemeResponseDTO scheme, Long projectId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();
            root.put("id", scheme.id());
            root.put("name", scheme.name());
            root.put("projectId", projectId);

            ObjectNode ntRules = root.putObject("ntRules");
            ntRules.putArray("regexPatterns");
            // ntRules.putArray("regexPatterns")
            //     .add("^Model\\s+[A-Z0-9]+$")
            //     .add("\\bBMW\\s+X[0-9]\\b");
            ntRules.putArray("staticTerms");
            // ntRules.putArray("staticTerms")
            //     .add("API").add("USB").add("ABS");
            ntRules.putArray("exactTerms");
            // ntRules.putArray("exactTerms")
            //     .add("OK").add("Cancel").add("Yes").add("No");
            ntRules.putArray("inlineElements");
            // ntRules.putArray("inlineElements")
            //     .add("xref").add("userinput");

            ArrayNode matchTypeRates = root.putArray("matchTypeRates");
            for (MatchTypeRateResponseDTO rate : scheme.matchTypeRates()) {
                ObjectNode rateNode = matchTypeRates.addObject();
                rateNode.put("matchType", rate.matchType().name());
                rateNode.put("transMemoryPercent", rate.transMemoryPercent() != null ? rate.transMemoryPercent() : 0L);
                // rateNode.put("nonTranslatablePercent", rate.nonTranslatablePercent() != null ? rate.nonTranslatablePercent() : 0L);
                // rateNode.put("machineTransPercent", rate.machineTransPercent() != null ? rate.machineTransPercent() : 0L);
            }
            String json = mapper.writeValueAsString(root);
            String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            log.info("sizingRequestJson sent to Tomato:\n{}", prettyJson);
            return json;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build sizingRequestJson", e);
        }
    }

    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }
}
