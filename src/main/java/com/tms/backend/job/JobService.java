package com.tms.backend.job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.tms.backend.dto.FileDownloadDTO;
import com.tms.backend.dto.JobDTO;
import com.tms.backend.dto.JobSoftDeleteDTO;
import com.tms.backend.dto.JobWorkflowStepDTO;
import com.tms.backend.dto.JobWorkflowStepEditDTO;
import com.tms.backend.dto.ProjectWithJobDTO;
import com.tms.backend.dto.TomatoSizingResponse;
import com.tms.backend.exception.ResourceNotFoundException;
import com.tms.backend.mapper.ProjectMapper;
import com.tms.backend.project.Project;
import com.tms.backend.project.ProjectRepository;
import com.tms.backend.project.ProjectService;
import com.tms.backend.tomato.FileConversionService;
import com.tms.backend.tomato.SizingService;
import com.tms.backend.user.User;
import com.tms.backend.user.UserRepository;
import com.tms.backend.workflowSteps.WorkflowStep;
import com.tms.backend.workflowSteps.WorkflowStepRepository;

import jakarta.transaction.Transactional;

@Service
public class JobService {
    
    private final JobRepository jobRepo;
    private final ProjectRepository projectRepo;
    private final UserRepository userRepo;
    private final WorkflowStepRepository wfRepo;
    private final JobWorkflowStepRepository jobWfRepo;
    private final SizingService sizingService;
    private final FileConversionService fileConversionService;
    private final ProjectService projectService;


    private final ProjectMapper projectMapper;

    @Value("${file.upload-dir}")
    private String baseUploadDir;

    private static final Logger logger = LoggerFactory.getLogger(JobService.class);
    

    public JobService(JobRepository jobRepo, ProjectRepository projectRepo, UserRepository userRepo, WorkflowStepRepository wfRepo, JobWorkflowStepRepository jobWfRepo, ProjectMapper projectMapper, SizingService sizingService, FileConversionService fileConversionService, ProjectService projectService){
        this.jobRepo = jobRepo;
        this.projectRepo = projectRepo;
        this.userRepo = userRepo;
        this.wfRepo = wfRepo;
        this.jobWfRepo = jobWfRepo;
        this.projectMapper = projectMapper;
        this.sizingService = sizingService;
        this.fileConversionService = fileConversionService;
        this.projectService = projectService;
    }

    @Transactional
    public JobDTO createJob(MultipartFile file, JobDTO jobDTO, String uid) throws IOException {
        return createJob(file, jobDTO, uid, null, false);
    }

    @Transactional
    public JobDTO createJob(MultipartFile file, JobDTO jobDTO, String uid, String projectFolder, Boolean useSizingApi) throws IOException {

        User currentUser = userRepo.findByUid(uid)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + uid));

        // if job is created from submitter portal -> access sizing api
        TomatoSizingResponse sizingApiResponse = null;
        if (useSizingApi) {
            sizingApiResponse = sizingService.sendFileToTomatoAPI(file);
        }

        // pass file name and size when creating the db data
        Job job = createJobFromDTO(jobDTO, currentUser, file.getOriginalFilename(), file.getSize(), sizingApiResponse);

        // save to get the generated id
        Job savedJob = jobRepo.save(job);

        // add zero padding to ids
        String zeroPaddedJobId = String.format("%03d", savedJob.getId());
        
        // build folder name
        String dateTimeFolder = java.time.LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss"));
        String jobFolder = "job-" + zeroPaddedJobId + "_" + dateTimeFolder;

        // If projectFolder not provided, generate it from project ID
        if (projectFolder == null) {
            String zeroPaddedProjId = String.format("%03d", jobDTO.projectId());
            projectFolder = "project-" + zeroPaddedProjId + "_" + dateTimeFolder;
        }

        fileConversionService.uploadAndConvertFile(file, projectFolder, jobFolder, savedJob);

        // Path baseDir = Paths.get(baseUploadDir);
        // Path relativePath = baseDir.relativize(filePath);
        // String relativePathString = relativePath.toString().replace("\\", "/");
        // savedJob.setFilePath(relativePathString);

        List<JobWorkflowStep> jobSteps = new ArrayList<>();
        if (jobDTO.workflowSteps() != null) {
            jobSteps = createWorkflowSteps(jobDTO.workflowSteps(), savedJob);
        }

        //save workflow steps
        List<JobWorkflowStep> savedSteps = jobWfRepo.saveAll(jobSteps);
        savedJob.setWorkflowSteps(new HashSet<>(savedSteps));

        // Save the job again with updated file paths
        savedJob = jobRepo.save(savedJob);

         return convertToDTO(savedJob);
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
                savedProject.getId(), // create a project id for the job
                null, // no workflowstep
                jobDTO.segmentCount(),
                jobDTO.pageCount(),
                jobDTO.wordCount(),
                jobDTO.characterCount(),
                null,
                LocalDateTime.now()
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
        String projectName = "Widget Project" + " " + "portal page" + " " + user.getEmail();
        project.setName(projectName);
        project.setSourceLang(jobDTO.sourceLang());
        project.setTargetLanguages(jobDTO.targetLangs());
        project.setCreatedBy(user.getFirstName() + " " + user.getLastName());
        project.setNote(note);
        return project;
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
                User wfStepProvider = userRepo.findByUid(stepDTO.providerUid())
                    .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
                jobWfStep.setProvider(wfStepProvider);
            }

            if (stepDTO.notifyUserUid() != null){
                User notifyUser = userRepo.findByUid(stepDTO.notifyUserUid())
                    .orElseThrow(() -> new ResourceNotFoundException("User to notify not found"));
                jobWfStep.setNotifyUser(notifyUser);
            }

            jobWfStep.setDueDate(stepDTO.dueDate());

            jobSteps.add(jobWfStep);
        }

        return jobSteps;
    }

    public List<JobDTO> getJobs() {
        List<Job> jobs = jobRepo.findAll();
        return jobs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Get job by ID
    public Job getJobById(Long jobId) {
        return jobRepo.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));
    }

    public List<JobDTO> getJobsByProjectId(Long id) {
        List<Job> jobs = jobRepo.findByProjectId(id);
        return jobs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<JobSoftDeleteDTO> getDeletedJobsByUser(String uid) {
    // Find all deleted jobs owned by this user
    List<Job> deletedJobs = jobRepo.findByJobOwnerUidAndDeletedTrue(uid);
    
    return deletedJobs.stream()
        .map(JobSoftDeleteDTO::from)
        .collect(Collectors.toList());
    }

    @Transactional
    public JobWorkflowStepDTO updateWorkflowStep(Long jobId, JobWorkflowStepEditDTO stepUpdate) {
        Job job = jobRepo.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        JobWorkflowStep wfStep = job.getWorkflowSteps().stream()
                .filter(ws -> ws.getId().equals(stepUpdate.id()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Workflow step not found with id: " + stepUpdate.id()));

        if (stepUpdate.providerUid() != null) {
            User provider = userRepo.findByUid(stepUpdate.providerUid())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + stepUpdate.providerUid()));
            wfStep.setProvider(provider);
        }

        if (stepUpdate.status() != null) {
            wfStep.setStatus(stepUpdate.status());
        }

        if (stepUpdate.dueDate() != null) {
            wfStep.setDueDate(stepUpdate.dueDate());
        }

        jobRepo.save(job);

        // project status automation
        projectService.checkAndUpdateProjectStatus(job.getProject().getId(), wfStep.getWorkflowStep().getName());

        return JobWorkflowStepDTO.from(wfStep);
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
        if (!job.getJobOwner().getUid().equals(uid) && 
            !job.getProject().getOwner().getUid().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You cannot delete this job");
        }

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
        if (!job.getJobOwner().getUid().equals(uid) && 
            !job.getProject().getOwner().getUid().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You cannot restore this job");
        }

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
         List<JobWorkflowStepDTO> stepDTOs = job.getWorkflowSteps().stream()
        .map(this::convertStepToDTO)  // Convert each entity to DTO
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
                job.getContentType(),
                job.getProject() != null ? job.getProject().getId() : null,
                stepDTOs,
                job.getSegmentCount(),
                job.getPageCount(),
                job.getWordCount(),
                job.getCharacterCount(),
                job.getProgress(),
                job.getCreateDate()
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

    public FileDownloadDTO getOriginalFileForDownload(Long jobId, String uid) {
        Job job = jobRepo.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));
        
        // Check authorization: user must own the job or the project
        if (!job.getJobOwner().getUid().equals(uid) && 
            !job.getProject().getOwner().getUid().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, 
                "You are not authorized to download this file");
        }
        
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
        if (!job.getJobOwner().getUid().equals(uid) && 
            !job.getProject().getOwner().getUid().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, 
                "You are not authorized to download this file");
        }

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
