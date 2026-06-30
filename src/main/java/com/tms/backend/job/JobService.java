package com.tms.backend.job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.tms.backend.dto.FileDownloadDTO;
import com.tms.backend.dto.JobCheckoutStatusDTO;
import com.tms.backend.dto.JobDTO;
import com.tms.backend.dto.JobSoftDeleteDTO;
import com.tms.backend.dto.JobWorkflowStepDTO;
import com.tms.backend.dto.JobWorkflowStepEditDTO;
import com.tms.backend.dto.ProjectWithJobDTO;
import com.tms.backend.dto.TomatoSizingResponse;
import com.tms.backend.email.EmailService;
import com.tms.backend.exception.ResourceNotFoundException;
import com.tms.backend.mapper.ProjectMapper;
import com.tms.backend.project.Project;
import com.tms.backend.project.ProjectRepository;
import com.tms.backend.project.ProjectService;
import com.tms.backend.role.RoleConstants;
import com.tms.backend.tomato.FileConversionService;
import com.tms.backend.tomato.SizingService;
import com.tms.backend.tomato.SizingWithXliffResult;
import com.tms.backend.user.User;
import com.tms.backend.user.UserRepository;
import com.tms.backend.workflowSteps.WorkflowStep;
import com.tms.backend.workflowSteps.WorkflowStepRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class JobService {
    
    private final JobRepository jobRepo;
    private final ProjectRepository projectRepo;
    private final UserRepository userRepo;
    private final WorkflowStepRepository wfRepo;
    private final JobWorkflowStepRepository jobWfRepo;
    private final JobCheckoutRepository jobCheckoutRepo;
    private final SizingService sizingService;
    private final FileConversionService fileConversionService;
    private final ProjectService projectService;
    private final EmailService emailService;

    private final ProjectMapper projectMapper;

    @Value("${file.upload-dir}")
    private String baseUploadDir;

    @Value("${job.checkout.ttl-hours}")
    private long checkoutTtlHours;

    private static final Logger logger = LoggerFactory.getLogger(JobService.class);


    public JobService(JobRepository jobRepo, ProjectRepository projectRepo, UserRepository userRepo, WorkflowStepRepository wfRepo, JobWorkflowStepRepository jobWfRepo, JobCheckoutRepository jobCheckoutRepo, ProjectMapper projectMapper, SizingService sizingService, FileConversionService fileConversionService, ProjectService projectService, EmailService emailService){
        this.jobRepo = jobRepo;
        this.projectRepo = projectRepo;
        this.userRepo = userRepo;
        this.wfRepo = wfRepo;
        this.jobWfRepo = jobWfRepo;
        this.jobCheckoutRepo = jobCheckoutRepo;
        this.projectMapper = projectMapper;
        this.sizingService = sizingService;
        this.fileConversionService = fileConversionService;
        this.projectService = projectService;
        this.emailService = emailService;
    }

    @Transactional
    public JobDTO createJob(MultipartFile file, JobDTO jobDTO, String uid) throws IOException {
        return createJob(file, jobDTO, uid, null, false, false);
    }

    @Transactional
    public JobDTO createJob(MultipartFile file, JobDTO jobDTO, String uid, String projectFolder, Boolean useSizingApi) throws IOException {
        return createJob(file, jobDTO, uid, projectFolder, useSizingApi, false);
    }

    @Transactional
    public JobDTO createJob(MultipartFile file, JobDTO jobDTO, String uid, String projectFolder, Boolean useSizingApi, Boolean performSizingDuringCreation) throws IOException {

        User currentUser = userRepo.findByUid(uid)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + uid));

        TomatoSizingResponse sizingApiResponse = null;
        String tomatoSizingJobId = null;

        boolean isDitaFile = file.getOriginalFilename() != null
                && file.getOriginalFilename().toLowerCase().endsWith(".xml");

        if (Boolean.TRUE.equals(performSizingDuringCreation) && isDitaFile) {
            // Sizing path: submit to Tomato, poll for result, defer XLIFF save to user choice
            SizingWithXliffResult sizingWithXliff = sizingService.sendDitaFileAndGetXliff(file);
            sizingApiResponse = sizingWithXliff.sizingResponse();
            tomatoSizingJobId = sizingWithXliff.tomatoJobId();
        } else if (Boolean.TRUE.equals(useSizingApi)) {
            // Submitter portal path: synchronous sizing without XLIFF return
            sizingApiResponse = sizingService.sendFilesToTomatoAPI(List.of(file));
        }

        // Create job entity with sizing stats (if any) and persist to get the generated ID
        Job job = createJobFromDTO(jobDTO, currentUser, file.getOriginalFilename(), file.getSize(), sizingApiResponse);
        Job savedJob = jobRepo.save(job);

        String projectFolderName = String.valueOf(jobDTO.projectId());
        String jobFolder = String.valueOf(savedJob.getId());

        if (Boolean.TRUE.equals(performSizingDuringCreation) && isDitaFile) {
            // Save only the original file; XLIFF will be saved when the user triggers it
            fileConversionService.saveOriginalFileOnly(file, projectFolderName, jobFolder, savedJob);
        } else {
            fileConversionService.uploadAndConvertFile(file, projectFolderName, jobFolder, savedJob);
        }

        List<JobWorkflowStep> jobSteps = new ArrayList<>();
        if (jobDTO.workflowSteps() != null) {
            jobSteps = createWorkflowSteps(jobDTO.workflowSteps(), savedJob);
        }

        List<JobWorkflowStep> savedSteps = jobWfRepo.saveAll(jobSteps);
        savedJob.setWorkflowSteps(new HashSet<>(savedSteps));

        // Persist file paths, sizing stats, and workflow steps
        savedJob = jobRepo.save(savedJob);

        return convertToDTO(savedJob, tomatoSizingJobId);
    }

    /**
     * Fetches the XLIFF generated by a prior sizing-during-creation job from Tomato,
     * saves it as the job's converted file, and persists the updated file paths.
     */
    @Transactional
    public JobDTO saveXliffFromSizing(Long jobId, String tomatoJobId) throws IOException {
        Job job = jobRepo.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        if (job.getProject() == null) {
            throw new IllegalStateException("Job must be associated with a project");
        }

        String projectFolderName = String.valueOf(job.getProject().getId());
        String jobFolderName = String.valueOf(job.getId());

        byte[] xliffBytes = sizingService.fetchXliffBytes(tomatoJobId);
        fileConversionService.saveXliffToConvertedDir(xliffBytes, projectFolderName, jobFolderName, job);

        job = jobRepo.save(job);
        logger.info("Saved XLIFF for job {} from tomatoJobId {}", jobId, tomatoJobId);

        sizingService.deleteSizingJob(tomatoJobId);

        return convertToDTO(job);
    }

    //create multiple jobs
    @Transactional
    public List<JobDTO> createJobs(List<MultipartFile> files, JobDTO jobDTO, String uid) throws IOException {
        return createJobs(files, jobDTO, uid, null, false, false);
    }

    @Transactional
    public List<JobDTO> createJobs(List<MultipartFile> files, JobDTO jobDTO, String uid, String projectFolder,
            Boolean useSizingApi) throws IOException {
        return createJobs(files, jobDTO, uid, projectFolder, useSizingApi, false);
    }

    @Transactional
    public List<JobDTO> createJobs(List<MultipartFile> files, JobDTO jobDTO, String uid, String projectFolder,
            Boolean useSizingApi, Boolean performSizingDuringCreation) throws IOException {
        List<JobDTO> createdJobs = new ArrayList<>();
        for (MultipartFile file : files) {
            JobDTO created = createJob(file, jobDTO, uid, projectFolder, useSizingApi, performSizingDuringCreation);
            createdJobs.add(created);
        }
        return createdJobs;
    }

    // for submitter portal
    @Transactional
    public ProjectWithJobDTO createProjectWithJobs(
            List<MultipartFile> files,
            String note,
            JobDTO jobDTO,
            String uid) throws IOException {

        // Validate user once
        User currentUser = userRepo.findByUid(uid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + uid));

        // 1. Create the project first
        Project project = createWidgetProject(note, jobDTO, currentUser);
        Project savedProject = projectRepo.save(project);

        // 2. Update jobDTO with the new project ID
        JobDTO updatedJobDTO = new JobDTO(
                null,
                jobDTO.sourceLang(),
                jobDTO.targetLangs(),
                currentUser.getUid(),
                currentUser.getFirstName() + " " + currentUser.getLastName(),
                null,
                null,
                null,
                null,
                null,
                null,
                null, //content type
                savedProject.getId(), // create a project id for the job
                null, // no workflowstep
                jobDTO.segmentCount(),
                jobDTO.pageCount(),
                jobDTO.wordCount(),
                jobDTO.characterCount(),
                null,
                LocalDateTime.now(),
                null,  // tomatoSizingJobId
                null, null, null, // checkoutUserId, checkoutUserName, checkoutAt
                null, // fileUpdatedAt
                0, 0
        );

        String dateTimeFolder = java.time.LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss"));

        //create project folder name
        String zeroPaddedProjId = String.format("%03d", savedProject.getId());
        String projectFolder = "project-" + zeroPaddedProjId + "_" + dateTimeFolder;

        // 3. Create jobs for each file
        List<JobDTO> createdJobs = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                JobDTO createdJob = createJob(file, updatedJobDTO, uid, projectFolder, true);
                createdJobs.add(createdJob);
            }
        }

        // 4. Return both project and jobs
        return new ProjectWithJobDTO(
                projectMapper.toFullDTO(savedProject),
                createdJobs);
    }

    private Project createWidgetProject(String note, JobDTO jobDTO, User user) {
        Project project = new Project();
        String projectName = "Widget Project" + " " + user.getEmail();
        project.setName(projectName);
        project.setSourceLang(jobDTO.sourceLang());
        project.setTargetLanguages(jobDTO.targetLangs());
        project.setCreatedBy(user.getFirstName() + " " + user.getLastName());
        project.setNote(note);
        return project;
    }

    /**
     * Upload a translated XLIFF file and save it to the translated directory.
     * Updates the job's translatedFilePath in the database.
     * 
     * @param file  The translated XLIFF file
     * @param jobId The job ID
     * @return Path to the saved translated file
     * @throws IOException             if file operations fail
     * @throws EntityNotFoundException if job not found
     */
    public Path uploadTranslatedFile(MultipartFile file, Long jobId) throws IOException {
        logger.info("Processing translated file upload for job ID: {}", jobId);

        // Fetch the job
        Job job = jobRepo.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with ID: " + jobId));

        // Validate that job has a project
        if (job.getProject() == null) {
            throw new IllegalStateException("Job must be associated with a project");
        }

        // Get project and job folder names
        String projectFolderName = String.valueOf(job.getProject().getId());
        String jobFolderName = String.valueOf(job.getId());

        // Save the translated file
        Path savedPath = saveTranslatedFile(file, projectFolderName, jobFolderName, job);

        // Save the updated job to database
        jobRepo.save(job);

        logger.info("Translated file saved and job updated successfully");
        return savedPath;
    }

    /**
     * Save the translated XLIFF file to the local filesystem.
     * 
     * @param file              The translated XLIFF file
     * @param projectFolderName The project folder name
     * @param jobFolderName     The job folder name
     * @param job               The job entity to update
     * @return Path to the saved file
     * @throws IOException if file operations fail
     */
    private Path saveTranslatedFile(
        MultipartFile file,
        String projectFolderName,
        String jobFolderName, 
        Job job) 
        throws IOException {
        // Get base directory
        Path baseDir = Paths.get(baseUploadDir);

        String convertedPath = job.getConvertedFilePath();
        Path convertedFullPath = Paths.get(convertedPath);
        // Get parent directory (removing "converted/filename.xliff")
        Path jobDirectory = convertedFullPath.getParent().getParent();
    
        // Create translated directory path
        Path outputDir = baseDir.resolve(jobDirectory).resolve("translated");

        // Create directory if it doesn't exist
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
            logger.info("Created translated directory: {}", outputDir.toAbsolutePath());
        }

        // Get filename
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            fileName = "translated_" + System.currentTimeMillis() + ".xliff";
        }

        // Create full output path
        Path outputPath = outputDir.resolve(fileName);

        // Write the file
        file.transferTo(outputPath.toFile());
        logger.info("Saved translated file: {}", outputPath.toAbsolutePath());

        // Calculate relative path
        Path relativePath = baseDir.relativize(outputPath);

        // Update job with translated file path
        job.setTranslatedFileName(fileName);
        job.setTranslatedFilePath(relativePath.toString().replace("\\", "/"));
        job.setFileUpdatedAt(LocalDateTime.now());

        logger.info("Updated job with translated file path: {}", job.getTranslatedFilePath());

        return outputPath;
    }

    private Job createJobFromDTO(JobDTO jobDTO, User currentUser, String fileName, Long fileSize, TomatoSizingResponse tomatoSizingStats) {
        Job job = new Job();
        job.setSourceLang(jobDTO.sourceLang());
        job.setTargetLangs(jobDTO.targetLangs());
        job.setContentType(jobDTO.contentType());
        job.setFileName(fileName);
        job.setFileSize(fileSize);
        if (jobDTO.progress() != null) {
            job.setProgress(jobDTO.progress().longValue());
        }
        // apply stats
        if (tomatoSizingStats != null) {
            job.setSegmentCount(tomatoSizingStats.statistics().totalSegments());
            job.setPageCount(0L);
            job.setWordCount(tomatoSizingStats.statistics().totalWords());
            job.setCharacterCount(tomatoSizingStats.statistics().totalCharacters());
        }

        job.setJobOwner(currentUser);

        if (jobDTO.projectId() != null) {
            Long projectId = jobDTO.projectId();
            Project project = projectRepo.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
            job.setProject(project);
        }

        return job;
    }

    private List<JobWorkflowStep> createWorkflowSteps(List<JobWorkflowStepDTO> stepDTOs, Job job) {
        List<JobWorkflowStep> jobSteps = new ArrayList<>();

        for (JobWorkflowStepDTO stepDTO : stepDTOs) {
            JobWorkflowStep jobWfStep = new JobWorkflowStep();
            jobWfStep.setJob(job);

            WorkflowStep wfStepReference = wfRepo.findById(stepDTO.workflowStepId())
                    .orElseThrow(() -> new ResourceNotFoundException("WorkflowStep not found"));
            jobWfStep.setWorkflowStep(wfStepReference);

            if (stepDTO.providerUid() != null){
                userRepo.findByUid(stepDTO.providerUid())
                    .ifPresent(jobWfStep::setProvider);
            }

            if (stepDTO.notifyUserUid() != null){
                userRepo.findByUid(stepDTO.notifyUserUid())
                    .ifPresent(jobWfStep::setNotifyUser);
            }

            jobWfStep.setDueDate(stepDTO.dueDate());

            jobSteps.add(jobWfStep);
        }

        return jobSteps;
    }

    @Transactional(readOnly = true)
    public List<JobDTO> getJobs(User user) {
        List<Job> jobs;

        if (RoleConstants.ADMIN.equals(user.getRole().getName()) || RoleConstants.PM.equals(user.getRole().getName())) {
            jobs = jobRepo.findAllActive();
        } else {
            // linguist (or any non-admin/PM role)
            jobs = jobRepo.findByJobOwnerIdAndDeletedFalse(user.getId());
        }
        return jobs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public JobDTO getJobDTOById(Long id) {
        Job job = jobRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
        return convertToDTO(job);
    }

    public JobCheckoutStatusDTO getCheckoutStatus(Long jobId) {
        Job job = jobRepo.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        JobCheckout checkout = jobCheckoutRepo.findByJobId(jobId)
                .filter(c -> !c.isExpired())
                .orElse(null);

        return buildCheckoutStatusDTO(job, checkout);
    }

    private JobCheckoutStatusDTO buildCheckoutStatusDTO(Job job, JobCheckout checkout) {
        if (checkout == null) {
            return new JobCheckoutStatusDTO(null, null, null, job.getFileUpdatedAt(), null, null);
        }
        return new JobCheckoutStatusDTO(
                checkout.getUserId(),
                checkout.getUserName(),
                checkout.getCheckedOutAt(),
                job.getFileUpdatedAt(),
                checkout.getLastSavedAt(),
                checkout.getExpiresAt()
        );
    }

    // Resolve the job's storage folder (".../{projectId}/{jobId}") from any of its known file paths
    private Path resolveJobDirectory(Job job) {
        String referencePath = job.getConvertedFilePath() != null
                ? job.getConvertedFilePath()
                : job.getOriginalFilePath();
        if (referencePath == null) {
            throw new ResourceNotFoundException("Job has no files on disk: " + job.getId());
        }
        return Paths.get(referencePath).getParent().getParent();
    }

    // Delete a checkout's working copy from disk and DB, and clear the job's cached lock fields
    private void releaseCheckout(Job job, JobCheckout checkout) {
        try {
            Path baseDir = Paths.get(baseUploadDir);
            Files.deleteIfExists(baseDir.resolve(checkout.getWorkingCopyPath()));
        } catch (IOException e) {
            logger.warn("Could not delete working copy for job {}: {}", job.getId(), e.getMessage());
        }
        jobCheckoutRepo.delete(checkout);
        job.setCheckoutUserId(null);
        job.setCheckoutUserName(null);
        job.setCheckoutAt(null);
        jobRepo.save(job);
    }

    // Get job by ID
    public Job getJobById(Long jobId) {
        return jobRepo.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));
    }

    public List<JobDTO> getJobsByProjectId(Long id) {
        List<Job> jobs = jobRepo.findByProjectIdAndDeletedFalse(id);
        return jobs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // return job entity
    List<Job> getJobEntitiesByProjectId(Long projectId) {
        return jobRepo.findByProjectIdAndDeletedFalse(projectId);
    }

    public List<JobSoftDeleteDTO> getDeletedJobsByUser(String uid) {
    // Find all deleted jobs owned by this user
    List<Job> deletedJobs = jobRepo.findByJobOwnerUidAndDeletedTrue(uid);
    
    return deletedJobs.stream()
        .map(JobSoftDeleteDTO::from)
        .collect(Collectors.toList());
    }

    @Transactional
    public Path generateTargetFile(Long jobId) throws IOException {
        Job job = jobRepo.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        Path relativeTargetPath =
            fileConversionService.convertXliffBackToOriginalFormat(
                job,
                job.getProject().getId().toString(),
                job.getId().toString()
            );

        // save 
        jobRepo.save(job);

        return relativeTargetPath;
    }

    @Transactional
    public JobWorkflowStepDTO updateWorkflowStep(Long jobId, JobWorkflowStepEditDTO stepUpdate, String currentUserUid) {
        Job job = jobRepo.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        JobWorkflowStep wfStep = job.getWorkflowSteps().stream()
                .filter(ws -> ws.getId().equals(stepUpdate.id()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Workflow step not found with id: " + stepUpdate.id()));

        // Capture previous status before updating
        JobWorkflowStatus previousStatus = null;
        boolean statusChanged = false;

        if (stepUpdate.providerUid() != null) {
            User provider = userRepo.findByUid(stepUpdate.providerUid())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + stepUpdate.providerUid()));
            wfStep.setProvider(provider);
        }

        if (stepUpdate.status() != null) {
            previousStatus = wfStep.getStatus();
            logger.info("previous stat {} new stat {} ", previousStatus, stepUpdate.status());
            if (!stepUpdate.status().equals(previousStatus)) {
                statusChanged = true;
                wfStep.setStatus(stepUpdate.status());
            }
        }

        if (stepUpdate.dueDate() != null) {
            wfStep.setDueDate(stepUpdate.dueDate());
        }

        jobRepo.save(job);

        // Send email notification if status changed
        if (statusChanged && job.getJobOwner() != null && job.getJobOwner().getEmail() != null) {
            logger.info("job owner id {}", job.getJobOwner().getEmail());
            emailService.sendJobStatusChangeEmail(
                    job.getJobOwner().getEmail(),
                    job.getProject().getName(),
                    wfStep.getWorkflowStep().getName(),
                    previousStatus,
                    stepUpdate.status());
        }

        // project status automation
        projectService.checkAndUpdateProjectStatus(job.getProject().getId(), wfStep.getWorkflowStep().getName(), currentUserUid);

        return JobWorkflowStepDTO.from(wfStep);
    }

    @Transactional
    public JobWorkflowStepDTO updateWorkflowStepStatus(Long jobId, Long stepId, com.tms.backend.job.JobWorkflowStatus status, String currentUserUid) {
        Job job = jobRepo.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        JobWorkflowStep wfStep = job.getWorkflowSteps().stream()
                .filter(ws -> ws.getId().equals(stepId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Workflow step not found with id: " + stepId));

        JobWorkflowStatus previousStatus = wfStep.getStatus();
        if (status.equals(previousStatus)) {
            return JobWorkflowStepDTO.from(wfStep);
        }

        wfStep.setStatus(status);
        jobRepo.save(job);

        if (job.getJobOwner() != null && job.getJobOwner().getEmail() != null) {
            emailService.sendJobStatusChangeEmail(
                    job.getJobOwner().getEmail(),
                    job.getProject().getName(),
                    wfStep.getWorkflowStep().getName(),
                    previousStatus,
                    status);
        }

        projectService.checkAndUpdateProjectStatus(job.getProject().getId(), wfStep.getWorkflowStep().getName(), currentUserUid);

        return JobWorkflowStepDTO.from(wfStep);
    }

    @Transactional
    public JobCheckoutStatusDTO checkoutJob(Long jobId, String uid) {
        Job job = jobRepo.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        Optional<JobCheckout> existingOpt = jobCheckoutRepo.findByJobId(jobId);
        if (existingOpt.isPresent()) {
            JobCheckout existing = existingOpt.get();
            if (existing.isExpired()) {
                releaseCheckout(job, existing);
            } else if (existing.getUserId().equals(uid)) {
                // Same user returning before expiry: resume, don't touch the working copy
                return buildCheckoutStatusDTO(job, existing);
            } else {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "File is already checked out by " + existing.getUserName());
            }
        }

        User currentUser = userRepo.findByUid(uid)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + uid));

        String sourcePath = job.getTranslatedFilePath() != null
                ? job.getTranslatedFilePath() : job.getConvertedFilePath();
        String sourceFileName = job.getTranslatedFileName() != null
                ? job.getTranslatedFileName() : job.getConvertedFileName();
        if (sourcePath == null) {
            throw new ResourceNotFoundException("Job has no converted file to check out: " + jobId);
        }

        Path baseDir = Paths.get(baseUploadDir);
        Path sourceAbsolutePath = baseDir.resolve(sourcePath);
        if (!Files.exists(sourceAbsolutePath)) {
            throw new ResourceNotFoundException("Source file not found on disk: " + sourceAbsolutePath);
        }

        try {
            Path workingCopyDir = baseDir.resolve(resolveJobDirectory(job)).resolve("working-copy");
            Files.createDirectories(workingCopyDir);
            Path workingCopyAbsolutePath = workingCopyDir.resolve(sourceFileName);
            Files.copy(sourceAbsolutePath, workingCopyAbsolutePath, StandardCopyOption.REPLACE_EXISTING);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusHours(checkoutTtlHours);
            String relativeWorkingCopyPath = baseDir.relativize(workingCopyAbsolutePath).toString().replace("\\", "/");
            String userName = currentUser.getFirstName() + " " + currentUser.getLastName();

            JobCheckout checkout = new JobCheckout(job, uid, userName, now, expiresAt,
                    relativeWorkingCopyPath, sourceFileName);
            jobCheckoutRepo.save(checkout);

            job.setCheckoutUserId(uid);
            job.setCheckoutUserName(userName);
            job.setCheckoutAt(now);
            jobRepo.save(job);

            return buildCheckoutStatusDTO(job, checkout);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to create working copy: " + e.getMessage());
        }
    }

    @Transactional
    public JobCheckoutStatusDTO saveDraft(Long jobId, String uid, MultipartFile file) {
        Job job = jobRepo.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        JobCheckout checkout = jobCheckoutRepo.findByJobId(jobId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job is not checked out: " + jobId));

        if (!checkout.getUserId().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Cannot save: checked out by another user");
        }

        try {
            Path baseDir = Paths.get(baseUploadDir);
            file.transferTo(baseDir.resolve(checkout.getWorkingCopyPath()).toFile());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to save working copy: " + e.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        checkout.setLastSavedAt(now);
        checkout.setExpiresAt(now.plusHours(checkoutTtlHours));
        jobCheckoutRepo.save(checkout);

        return buildCheckoutStatusDTO(job, checkout);
    }

    // Get the working copy file path for the editor to load while checked out
    public Path getWorkingCopyPath(Long jobId) {
        JobCheckout checkout = jobCheckoutRepo.findByJobId(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job is not checked out: " + jobId));

        Path baseDir = Paths.get(baseUploadDir);
        Path filePath = baseDir.resolve(checkout.getWorkingCopyPath());

        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Working copy not found on disk: " + filePath);
        }

        return filePath;
    }

    @Transactional
    public JobDTO checkinJob(Long jobId, String uid, MultipartFile file) {
        Job job = jobRepo.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        JobCheckout checkout = jobCheckoutRepo.findByJobId(jobId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job is not checked out: " + jobId));

        if (!checkout.getUserId().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Cannot check in a file checked out by another user");
        }

        Path baseDir = Paths.get(baseUploadDir);
        Path workingCopyAbsolutePath = baseDir.resolve(checkout.getWorkingCopyPath());

        try {
            // Defensive: commit any final edit that wasn't already saved as a draft
            if (file != null && !file.isEmpty()) {
                file.transferTo(workingCopyAbsolutePath.toFile());
            }

            String translatedFileName = job.getTranslatedFileName() != null
                    ? job.getTranslatedFileName() : checkout.getWorkingCopyFileName();

            Path translatedDir = baseDir.resolve(resolveJobDirectory(job)).resolve("translated");
            Files.createDirectories(translatedDir);
            Path translatedAbsolutePath = translatedDir.resolve(translatedFileName);
            Files.copy(workingCopyAbsolutePath, translatedAbsolutePath, StandardCopyOption.REPLACE_EXISTING);

            job.setTranslatedFileName(translatedFileName);
            job.setTranslatedFilePath(baseDir.relativize(translatedAbsolutePath).toString().replace("\\", "/"));
            job.setFileUpdatedAt(LocalDateTime.now());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to commit working copy: " + e.getMessage());
        }

        job.setCheckoutUserId(null);
        job.setCheckoutUserName(null);
        job.setCheckoutAt(null);
        jobRepo.save(job);

        try {
            Files.deleteIfExists(workingCopyAbsolutePath);
        } catch (IOException e) {
            logger.warn("Could not delete working copy on check-in for job {}: {}", jobId, e.getMessage());
        }
        jobCheckoutRepo.delete(checkout);

        return convertToDTO(job);
    }

    @Transactional
    public JobDTO cancelCheckoutJob(Long jobId, String uid) {
        Job job = jobRepo.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        JobCheckout checkout = jobCheckoutRepo.findByJobId(jobId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job is not checked out: " + jobId));

        if (!checkout.getUserId().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Cannot cancel checkout of a file checked out by another user");
        }

        // Discard never touches the committed translated file - only the lock and working copy
        releaseCheckout(job, checkout);

        return convertToDTO(job);
    }

    // Periodically reclaim locks/working copies nobody re-checked-out after they expired
    @Scheduled(fixedRateString = "${job.checkout.cleanup-interval-ms}")
    @Transactional
    public void cleanupExpiredCheckouts() {
        List<JobCheckout> expired = jobCheckoutRepo.findByExpiresAtBefore(LocalDateTime.now());
        for (JobCheckout checkout : expired) {
            Job job = checkout.getJob();
            try {
                Path baseDir = Paths.get(baseUploadDir);
                Files.deleteIfExists(baseDir.resolve(checkout.getWorkingCopyPath()));
            } catch (IOException e) {
                logger.warn("Could not delete expired working copy for job {}: {}",
                        job != null ? job.getId() : null, e.getMessage());
            }
            if (job != null) {
                job.setCheckoutUserId(null);
                job.setCheckoutUserName(null);
                job.setCheckoutAt(null);
                jobRepo.save(job);
            }
            jobCheckoutRepo.delete(checkout);
        }
        if (!expired.isEmpty()) {
            logger.info("Cleaned up {} expired job checkout(s)", expired.size());
        }
    }

    public void deleteJob(Long id) throws IOException {
        if (!jobRepo.existsById(id)){
            throw new ResourceNotFoundException("Job not found with id: " + id);
        }
        
        Job job = getJobById(id);
        
        Path baseDir = Paths.get(baseUploadDir);
    
        // Delete original file from disk
        if (job.getOriginalFilePath() != null) {
            Path originalPath = baseDir.resolve(job.getOriginalFilePath());
            Files.deleteIfExists(originalPath);
            logger.info("Deleted original file: {}", originalPath);
        }

        // Delete converted file from disk
        if (job.getConvertedFilePath() != null) {
            Path convertedPath = baseDir.resolve(job.getConvertedFilePath());
            Files.deleteIfExists(convertedPath);
            logger.info("Deleted converted file: {}", convertedPath);
        }

        // Optional: Delete the entire job folder if it's empty
        if (job.getOriginalFilePath() != null) {
            Path jobFolder = baseDir.resolve(job.getOriginalFilePath()).getParent().getParent();
            deleteEmptyDirectories(jobFolder);
        }

        // Delete the job from database (this will cascade to related entities if
        // configured)
        // jobRepo.deleteById(id);
        jobRepo.delete(job);
        logger.info("Deleted job with id: {}", id);
    }

    // Helper method to clean up empty directories
    private void deleteEmptyDirectories(Path directory) throws IOException {
        if (Files.exists(directory) && Files.isDirectory(directory)) {
            // Check if directory is empty
            try (var stream = Files.list(directory)) {
                if (stream.findAny().isEmpty()) {
                    Files.delete(directory);
                    logger.info("Deleted empty directory: {}", directory);

                    // Recursively delete parent if empty
                    Path parent = directory.getParent();
                    if (parent != null && !parent.equals(Paths.get(baseUploadDir))) {
                        deleteEmptyDirectories(parent);
                    }
                }
            }
        }
    }

    @Transactional
    public void softDeleteJob(Long jobId, String uid) {
        Job job = jobRepo.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        // Optional: Check if user owns the job or the project
        // if (!job.getJobOwner().getUid().equals(uid) && 
        //     !job.getProject().getOwner().getUid().equals(uid)) {
        //     throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You cannot delete this job");
        // }

        // Get current user for deletedBy field
        User currentUser = userRepo.findByUid(uid)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Soft delete the job only
        job.setDeleted(true);
        job.setDeletedDate(LocalDateTime.now());
        job.setDeletedBy(currentUser.getFirstName() + " " + currentUser.getLastName());
        jobRepo.save(job);
    }

    @Transactional
    public JobDTO restoreJob(Long jobId, String uid) {
        Job job = jobRepo.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        // Optional: Check if user owns the job or the project
        // if (!job.getJobOwner().getUid().equals(uid) && 
        //     !job.getProject().getOwner().getUid().equals(uid)) {
        //     throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You cannot restore this job");
        // }

        if (!job.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job is not deleted");
        }

        // Check if parent project is deleted
        if (job.getProject().isDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Cannot restore job because its project is deleted. Restore the project first.");
        }

        // Restore the job
        job.setDeleted(false);
        job.setDeletedDate(null);
        job.setDeletedBy(null);
        jobRepo.save(job);

        return convertToDTO(job);
    }

    public JobDTO convertToDTO(Job job) {
        return convertToDTO(job, null);
    }

    public JobDTO convertToDTO(Job job, String tomatoSizingJobId) {
        List<JobWorkflowStepDTO> stepDTOs = job.getWorkflowSteps().stream()
                .map(this::convertStepToDTO)
                .toList();

        String ownerUid = null;
        String ownerName = null;
        if (job.getJobOwner() != null) {
            ownerUid = job.getJobOwner().getUid();
            ownerName = job.getJobOwner().getFirstName() + " " + job.getJobOwner().getLastName();
        }

        return new JobDTO(
                job.getId(),
                job.getSourceLang(),
                job.getTargetLangs(),
                ownerUid,
                ownerName,
                job.getFileName(),
                job.getFileSize(),
                job.getOriginalFilePath(),
                job.getConvertedFilePath(),
                job.getTranslatedFilePath(),
                job.getTargetFilePath(),
                job.getContentType(),
                job.getProject() != null ? job.getProject().getId() : null,
                stepDTOs,
                job.getSegmentCount(),
                job.getPageCount(),
                job.getWordCount(),
                job.getCharacterCount(),
                job.getProgress(),
                job.getCreateDate(),
                tomatoSizingJobId,
                job.getCheckoutUserId(),
                job.getCheckoutUserName(),
                job.getCheckoutAt(),
                job.getFileUpdatedAt(),
                job.getVersionMajor(),
                job.getVersionMinor()
        );
    }

    private JobWorkflowStepDTO convertStepToDTO(JobWorkflowStep wfStep) {
        return JobWorkflowStepDTO.from(wfStep);
    }

    // Get original file path
    public Path getOriginalFilePath(Long jobId) {
        Job job = getJobById(jobId);
        
        if (job.getOriginalFilePath() == null) {
            throw new ResourceNotFoundException("No original file found for job: " + jobId);
        }
        
        Path baseDir = Paths.get(baseUploadDir);
        Path filePath = baseDir.resolve(job.getOriginalFilePath());
        
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Original file not found on disk: " + filePath);
        }
        
        return filePath;
    }
    
    // Get converted file path
    public Path getConvertedFilePath(Long jobId) {
        Job job = getJobById(jobId);
        
        if (job.getConvertedFilePath() == null) {
            throw new ResourceNotFoundException("No converted file found for job: " + jobId);
        }
        
        Path baseDir = Paths.get(baseUploadDir);
        Path filePath = baseDir.resolve(job.getConvertedFilePath());
        
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Converted file not found on disk: " + filePath);
        }
        
        return filePath;
    }

    // Get translated file path
    public Path getTranslatedFilePath(Long jobId) {
        Job job = getJobById(jobId);
        
        if (job.getTranslatedFilePath() == null) {
            throw new ResourceNotFoundException("No translated file found for job: " + jobId);
        }
        
        Path baseDir = Paths.get(baseUploadDir);
        Path filePath = baseDir.resolve(job.getTranslatedFilePath());
        
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Translated file not found on disk: " + filePath);
        }

        return filePath;
    }


    public FileDownloadDTO getOriginalFileForDownload(Long jobId, String uid) {
        Job job = jobRepo.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));
        
        // Check authorization: user must own the job or the project
        // if (!job.getJobOwner().getUid().equals(uid) && 
        //     !job.getProject().getOwner().getUid().equals(uid)) {
        //     throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, 
        //         "You are not authorized to download this file");
        // }
        
        // Check if file path exists
        if (job.getOriginalFilePath() == null) {
            throw new ResourceNotFoundException("No original file found for job: " + jobId);
        }
        
        // Verify file exists on disk
        Path baseDir = Paths.get(baseUploadDir);
        Path filePath = baseDir.resolve(job.getOriginalFilePath());
        
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Original file not found on disk: " + filePath);
        }
        
        return new FileDownloadDTO(
            job.getOriginalFileName() != null ? job.getOriginalFileName() : job.getFileName(),
            job.getContentType(),
            job.getOriginalFilePath()
        );
    }

    public FileDownloadDTO getConvertedFileForDownload(Long jobId, String uid) {
        Job job = jobRepo.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        // Check authorization
        // if (!job.getJobOwner().getUid().equals(uid) && 
        //     !job.getProject().getOwner().getUid().equals(uid)) {
        //     throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, 
        //         "You are not authorized to download this file");
        // }

        // Check if converted file exists
        if (job.getConvertedFilePath() == null) {
            throw new ResourceNotFoundException("No converted file found for job: " + jobId);
        }

        // Verify file exists on disk
        Path baseDir = Paths.get(baseUploadDir);
        Path filePath = baseDir.resolve(job.getConvertedFilePath());

        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Converted file not found on disk: " + filePath);
        }

        return new FileDownloadDTO(
            job.getConvertedFileName() != null ? job.getConvertedFileName() : job.getFileName(),
            job.getContentType(),
            job.getConvertedFilePath()
        );
    }
}
