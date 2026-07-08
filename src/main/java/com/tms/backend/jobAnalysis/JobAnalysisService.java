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
    public String initiateSizing(List<Long> jobIds, User user) {
        List<Job> jobs = jobIds.stream()
                .map(id -> jobRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Job not found with id: " + id)))
                .collect(Collectors.toList());

        Job primaryJob = jobs.get(0);
        Project project = primaryJob.getProject();
        Long projectId = project.getId();

        NetRateSchemeResponseDTO scheme = resolveSchemeForProject(project);

        Long tmId = tmAssignmentRepo
                .findFirstByProjectIdAndReadAccessTrue(projectId)
                .map(ProjectTmAssignment::getTmId)
                .orElseThrow(() -> new RuntimeException(
                        "No TM with read access found for project " + projectId));

        log.info("Sizing request for project {} using tmId: {}", projectId, tmId);

        String sizingRequestJson = buildSizingRequestJson(scheme, projectId);

        List<String> filePaths = jobs.stream()
                .map(Job::getOriginalFilePath)
                .collect(Collectors.toList());

        String sourceLanguage = primaryJob.getSourceLang();
        String targetLanguage = primaryJob.getTargetLangs() != null
                ? primaryJob.getTargetLangs().stream().findFirst().orElse(null)
                : null;

        String tomatoJobId = sizingService.sendFilesToTomatoAPIByPath(
                filePaths, sizingRequestJson, tmId, sourceLanguage, targetLanguage);

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

        // logger for segments
        // log.info("Sizing stats - repetition: {}, contextMatch: {}, perfect100: {}, fuzzy95: {}, fuzzy85: {}, fuzzy75: {}, fuzzy50: {}, noMatch: {}",
        //         stats.repetitionSegments(), stats.contextMatchSegments(), stats.perfect100Segments(),
        //         stats.fuzzy95Segments(), stats.fuzzy85Segments(), stats.fuzzy75Segments(),
        //         stats.fuzzy50Segments(), stats.noMatchSegments());

        jobAnalysis.setSourceLang(stats.sourceLanguage());
        if (stats.targetLanguage() != null) {
            jobAnalysis.setTargetLanguages(java.util.Set.of(stats.targetLanguage()));
        }

        // words
        jobAnalysis.setRepetitionWords(safeLong(stats.repetitionCount()));
        jobAnalysis.setContextMatchWords(safeLong(stats.context101TM_Words()) + safeLong(stats.context101NT_Words()));
        jobAnalysis.setPerfect100Words(safeLong(stats.perfect100Count()));
        jobAnalysis.setFuzzy95Words(safeLong(stats.fuzzy95Count()));
        jobAnalysis.setFuzzy75Words(safeLong(stats.fuzzy75Count()));
        jobAnalysis.setFuzzy50Words(safeLong(stats.fuzzy50Count()));
        jobAnalysis.setNoMatchWords(safeLong(stats.noMatchCount()));
        jobAnalysis.setFuzzy85Words(safeLong(stats.fuzzy85Count()));
        jobAnalysis.setAllWords(stats.totalCount());

        // TM/NT breakdown of word counts
        jobAnalysis.setPerfect100WordsTM(safeLong(stats.perfect100TM_Words()));
        jobAnalysis.setPerfect100WordsNT(safeLong(stats.perfect100NT_Words()));
        jobAnalysis.setFuzzy95WordsTM(safeLong(stats.fuzzy95TM_Words()));
        jobAnalysis.setFuzzy95WordsNT(safeLong(stats.fuzzy95NT_Words()));
        jobAnalysis.setFuzzy85WordsTM(safeLong(stats.fuzzy85TM_Words()));
        jobAnalysis.setFuzzy85WordsNT(safeLong(stats.fuzzy85NT_Words()));
        jobAnalysis.setFuzzy75WordsTM(safeLong(stats.fuzzy75TM_Words()));
        jobAnalysis.setFuzzy75WordsNT(safeLong(stats.fuzzy75NT_Words()));
        jobAnalysis.setFuzzy50WordsTM(safeLong(stats.fuzzy50TM_Words()));
        jobAnalysis.setFuzzy50WordsNT(safeLong(stats.fuzzy50NT_Words()));
        jobAnalysis.setNoMatchWordsTM(safeLong(stats.noMatchTM_Words()));
        jobAnalysis.setNoMatchWordsNT(safeLong(stats.noMatchNT_Words()));

        // characters
        jobAnalysis.setRepetitionCharacters(safeLong(stats.repetitionTM_Characters() + safeLong(stats.repetitionNT_Characters())));
        jobAnalysis.setContextMatchCharacters(safeLong(stats.context101TM_Characters()) + safeLong(stats.context101NT_Characters()));
        jobAnalysis.setPerfect100Characters(safeLong(stats.perfect100TM_Characters()) + safeLong(stats.perfect100NT_Characters()));
        jobAnalysis.setFuzzy85Characters(safeLong(stats.fuzzy85TM_Characters()) + safeLong(stats.fuzzy85NT_Characters()));
        jobAnalysis.setFuzzy75Characters(safeLong(stats.fuzzy75TM_Characters()) + safeLong(stats.fuzzy75NT_Characters()));
        jobAnalysis.setFuzzy50Characters(safeLong(stats.fuzzy50TM_Characters()) + safeLong(stats.fuzzy50NT_Characters()));
        jobAnalysis.setNoMatchCharacters(safeLong(stats.noMatchTM_Characters()) + safeLong(stats.noMatchNT_Characters()));
        jobAnalysis.setFuzzy95Characters(safeLong(stats.fuzzy95TM_Characters()) + safeLong(stats.fuzzy95NT_Characters()));
        jobAnalysis.setAllCharacters(stats.totalCharacters());

        // TM breakdown of character counts
        jobAnalysis.setPerfect100CharactersTM(safeLong(stats.perfect100TM_Characters()));
        jobAnalysis.setFuzzy95CharactersTM(safeLong(stats.fuzzy95TM_Characters()));
        jobAnalysis.setFuzzy85CharactersTM(safeLong(stats.fuzzy85TM_Characters()));
        jobAnalysis.setFuzzy75CharactersTM(safeLong(stats.fuzzy75TM_Characters()));
        jobAnalysis.setFuzzy50CharactersTM(safeLong(stats.fuzzy50TM_Characters()));
        jobAnalysis.setNoMatchCharactersTM(safeLong(stats.noMatchTM_Characters()));

        // NT breakdown of character counts
        jobAnalysis.setPerfect100CharactersNT(safeLong(stats.perfect100NT_Characters()));
        jobAnalysis.setFuzzy95CharactersNT(safeLong(stats.fuzzy95NT_Characters()));
        jobAnalysis.setFuzzy85CharactersNT(safeLong(stats.fuzzy85NT_Characters()));
        jobAnalysis.setFuzzy75CharactersNT(safeLong(stats.fuzzy75NT_Characters()));
        jobAnalysis.setFuzzy50CharactersNT(safeLong(stats.fuzzy50NT_Characters()));
        jobAnalysis.setNoMatchCharactersNT(safeLong(stats.noMatchNT_Characters()));

        // segments (not supported for now)
        jobAnalysis.setRepetitionSegments(safeLong(stats.repetitionTM_Segments()));
        jobAnalysis.setContextMatchSegments(safeLong(stats.context101TM_Segments()));
        jobAnalysis.setPerfect100Segments(safeLong(stats.perfect100TM_Segments()));
        jobAnalysis.setFuzzy95Segments(safeLong(stats.fuzzy95TM_Segments()));
        jobAnalysis.setFuzzy85Segments(safeLong(stats.fuzzy85TM_Segments()));
        jobAnalysis.setFuzzy75Segments(safeLong(stats.fuzzy75TM_Segments()));
        jobAnalysis.setFuzzy50Segments(safeLong(stats.fuzzy50TM_Segments()));
        jobAnalysis.setNoMatchSegments(safeLong(stats.noMatchTM_Segments()));

        // for net rate breakdown
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
        jobAnalysis.setUnitType(stats.unitType());
        jobAnalysis.setTmNames(stats.tmNames());

        // log.info("Weighted values from Tomato - noMatchTM: {}, noMatchNT: {}, totalWeighted: {}, totalWeightedPct: {}",
        //         stats.noMatchTM_Weighted(), stats.noMatchNT_Weighted(), stats.totalWeighted(), stats.totalWeightedPercentage());

        // // logger for TM
        // log.info("TM Words - repetition: {}, contextMatch: {}, perfect100: {}, fuzzy95: {}, fuzzy85: {}, fuzzy75: {}, fuzzy50: {}, noMatch: {}",
        //         jobAnalysis.getRepetitionWords(), jobAnalysis.getContextMatchWords(), jobAnalysis.getPerfect100Words(),
        //         jobAnalysis.getFuzzy95Words(), jobAnalysis.getFuzzy85Words(), jobAnalysis.getFuzzy75Words(),
        //         jobAnalysis.getFuzzy50Words(), jobAnalysis.getNoMatchWords());

        log.info("Sizing mapping check [words]\n"
                + "  repetition   raw(repetitionCount)={} -> mapped={}\n"
                + "  contextMatch raw(context101TM_Words)={} -> mapped={}\n"
                + "  perfect100   raw(perfect100Count)={} -> mapped={}\n"
                + "  fuzzy95      raw(fuzzy95Count)={} -> mapped={}\n"
                + "  fuzzy85      raw(fuzzy85Count)={} -> mapped={}\n"
                + "  fuzzy75      raw(fuzzy75Count)={} -> mapped={}\n"
                + "  fuzzy50      raw(fuzzy50Count)={} -> mapped={}\n"
                + "  noMatch      raw(noMatchCount)={} -> mapped={}\n"
                + "  allWords     raw(totalCount)={} -> mapped={}",
                stats.repetitionCount(), jobAnalysis.getRepetitionWords(),
                stats.context101TM_Words(), jobAnalysis.getContextMatchWords(),
                stats.perfect100Count(), jobAnalysis.getPerfect100Words(),
                stats.fuzzy95Count(), jobAnalysis.getFuzzy95Words(),
                stats.fuzzy85Count(), jobAnalysis.getFuzzy85Words(),
                stats.fuzzy75Count(), jobAnalysis.getFuzzy75Words(),
                stats.fuzzy50Count(), jobAnalysis.getFuzzy50Words(),
                stats.noMatchCount(), jobAnalysis.getNoMatchWords(),
                stats.totalCount(), jobAnalysis.getAllWords());

        log.info("Sizing mapping check [characters]\n"
                + "  repetition    raw(repetitionTM_Characters+repetitionNT_Characters)={} -> mapped={}\n"
                + "  contextMatch  raw(context101TM_Characters)={} -> mapped={}\n"
                + "  perfect100    raw(perfect100TM_Characters+perfect100NT_Characters)={} -> mapped={}\n"
                + "  fuzzy95       raw(fuzzy95TM_Characters+fuzzy95NT_Characters)={} -> mapped={}\n"
                + "  fuzzy85       raw(fuzzy85TM_Characters+fuzzy85NT_Characters)={} -> mapped={}\n"
                + "  fuzzy75       raw(fuzzy75TM_Characters+fuzzy75NT_Characters)={} -> mapped={}\n"
                + "  fuzzy50       raw(fuzzy50TM_Characters+fuzzy50NT_Characters)={} -> mapped={}\n"
                + "  noMatch       raw(noMatchTM_Characters+noMatchNT_Characters)={} -> mapped={}\n"
                + "  allCharacters raw(totalCharacters)={} -> mapped={}",
                safeLong(stats.repetitionTM_Characters()) + safeLong(stats.repetitionNT_Characters()), jobAnalysis.getRepetitionCharacters(),
                stats.context101TM_Characters(), jobAnalysis.getContextMatchCharacters(),
                safeLong(stats.perfect100TM_Characters()) + safeLong(stats.perfect100NT_Characters()), jobAnalysis.getPerfect100Characters(),
                safeLong(stats.fuzzy95TM_Characters()) + safeLong(stats.fuzzy95NT_Characters()), jobAnalysis.getFuzzy95Characters(),
                safeLong(stats.fuzzy85TM_Characters()) + safeLong(stats.fuzzy85NT_Characters()), jobAnalysis.getFuzzy85Characters(),
                safeLong(stats.fuzzy75TM_Characters()) + safeLong(stats.fuzzy75NT_Characters()), jobAnalysis.getFuzzy75Characters(),
                safeLong(stats.fuzzy50TM_Characters()) + safeLong(stats.fuzzy50NT_Characters()), jobAnalysis.getFuzzy50Characters(),
                safeLong(stats.noMatchTM_Characters()) + safeLong(stats.noMatchNT_Characters()), jobAnalysis.getNoMatchCharacters(),
                stats.totalCharacters(), jobAnalysis.getAllCharacters());

        log.info("Sizing mapping check [segments]\n"
                + "  repetition   raw(repetitionTM_Segments)={} -> mapped={}\n"
                + "  contextMatch raw(context101TM_Segments)={} -> mapped={}\n"
                + "  perfect100   raw(perfect100TM_Segments)={} -> mapped={}\n"
                + "  fuzzy95      raw(fuzzy95TM_Segments)={} -> mapped={}\n"
                + "  fuzzy85      raw(fuzzy85TM_Segments)={} -> mapped={}\n"
                + "  fuzzy75      raw(fuzzy75TM_Segments)={} -> mapped={}\n"
                + "  fuzzy50      raw(fuzzy50TM_Segments)={} -> mapped={}\n"
                + "  noMatch      raw(noMatchTM_Segments)={} -> mapped={}",
                stats.repetitionTM_Segments(), jobAnalysis.getRepetitionSegments(),
                stats.context101TM_Segments(), jobAnalysis.getContextMatchSegments(),
                stats.perfect100TM_Segments(), jobAnalysis.getPerfect100Segments(),
                stats.fuzzy95TM_Segments(), jobAnalysis.getFuzzy95Segments(),
                stats.fuzzy85TM_Segments(), jobAnalysis.getFuzzy85Segments(),
                stats.fuzzy75TM_Segments(), jobAnalysis.getFuzzy75Segments(),
                stats.fuzzy50TM_Segments(), jobAnalysis.getFuzzy50Segments(),
                stats.noMatchTM_Segments(), jobAnalysis.getNoMatchSegments());

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
                file.setApprovedTM_Words(safeLong(s.approvedTM_Words()));
                file.setApprovedTM_Characters(safeLong(s.approvedTM_Characters()));
                file.setApprovedTM_Segments(safeLong(s.approvedTM_Segments()));
                file.setRepetitionTM_Words(safeLong(s.repetitionTM_Words()) + safeLong(s.repetitionNT_Words()));
                file.setRepetitionTM_Characters(safeLong(s.repetitionTM_Characters()) + safeLong(s.repetitionNT_Characters()));
                file.setRepetitionTM_Segments(safeLong(s.repetitionTM_Segments()));
                file.setContext101TM_Words(safeLong(s.context101TM_Words()) + safeLong(s.context101NT_Words()));
                file.setContext101TM_Characters(safeLong(s.context101TM_Characters()) + safeLong(s.context101NT_Characters()));
                file.setContext101TM_Segments(safeLong(s.context101TM_Segments()));
                file.setPerfect100TM_Words(safeLong(s.perfect100TM_Words()));
                file.setPerfect100TM_Characters(safeLong(s.perfect100TM_Characters()));
                file.setPerfect100TM_Segments(safeLong(s.perfect100TM_Segments()));
                file.setPerfect100NT_Words(safeLong(s.perfect100NT_Words()));
                file.setPerfect100NT_Characters(safeLong(s.perfect100NT_Characters()));
                file.setFuzzy95TM_Words(safeLong(s.fuzzy95TM_Words()));
                file.setFuzzy95TM_Characters(safeLong(s.fuzzy95TM_Characters()));
                file.setFuzzy95TM_Segments(safeLong(s.fuzzy95TM_Segments()));
                file.setFuzzy95NT_Words(safeLong(s.fuzzy95NT_Words()));
                file.setFuzzy95NT_Characters(safeLong(s.fuzzy95NT_Characters()));
                file.setFuzzy85TM_Words(safeLong(s.fuzzy85TM_Words()));
                file.setFuzzy85TM_Characters(safeLong(s.fuzzy85TM_Characters()));
                file.setFuzzy85TM_Segments(safeLong(s.fuzzy85TM_Segments()));
                file.setFuzzy85NT_Words(safeLong(s.fuzzy85NT_Words()));
                file.setFuzzy85NT_Characters(safeLong(s.fuzzy85NT_Characters()));
                file.setFuzzy75TM_Words(safeLong(s.fuzzy75TM_Words()));
                file.setFuzzy75TM_Characters(safeLong(s.fuzzy75TM_Characters()));
                file.setFuzzy75TM_Segments(safeLong(s.fuzzy75TM_Segments()));
                file.setFuzzy75NT_Words(safeLong(s.fuzzy75NT_Words()));
                file.setFuzzy75NT_Characters(safeLong(s.fuzzy75NT_Characters()));
                file.setFuzzy50TM_Words(safeLong(s.fuzzy50TM_Words()));
                file.setFuzzy50TM_Characters(safeLong(s.fuzzy50TM_Characters()));
                file.setFuzzy50TM_Segments(safeLong(s.fuzzy50TM_Segments()));
                file.setFuzzy50NT_Words(safeLong(s.fuzzy50NT_Words()));
                file.setFuzzy50NT_Characters(safeLong(s.fuzzy50NT_Characters()));
                file.setNoMatchTM_Words(safeLong(s.noMatchTM_Words()) + safeLong(s.noMatchNT_Words()));
                file.setNoMatchTM_Characters(safeLong(s.noMatchTM_Characters()) + safeLong(s.noMatchNT_Characters()));
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
                file.setAllWords(s.totalCount());
                file.setAllCharacters(s.totalCharacters());
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