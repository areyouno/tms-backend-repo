package com.tms.backend.job;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.tms.backend.dto.FileDownloadDTO;
import com.tms.backend.dto.JobDTO;
import com.tms.backend.dto.JobWorkflowStepDTO;
import com.tms.backend.dto.JobWorkflowStepEditDTO;
import com.tms.backend.dto.ProjectWithJobDTO;
import com.tms.backend.dto.TomatoSizingResponse;
import com.tms.backend.exception.ResourceNotFoundException;
import com.tms.backend.mapper.ProjectMapper;
import com.tms.backend.project.Project;
import com.tms.backend.project.ProjectRepository;
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

    private final ProjectMapper projectMapper;

    @Value("${file.upload-dir}")
    private String baseUploadDir;
    

    public JobService(JobRepository jobRepo, ProjectRepository projectRepo, UserRepository userRepo, WorkflowStepRepository wfRepo, JobWorkflowStepRepository jobWfRepo, ProjectMapper projectMapper, SizingService sizingService, FileConversionService fileConversionService){
        this.jobRepo = jobRepo;
        this.projectRepo = projectRepo;
        this.userRepo = userRepo;
        this.wfRepo = wfRepo;
        this.jobWfRepo = jobWfRepo;
        this.projectMapper = projectMapper;
        this.sizingService = sizingService;
        this.fileConversionService = fileConversionService;
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

        Path filePath = fileConversionService.uploadAndConvertFile(file, projectFolder, jobFolder);

        Path baseDir = Paths.get(baseUploadDir);
        Path relativePath = baseDir.relativize(filePath);
        String relativePathString = relativePath.toString().replace("\\", "/");
        savedJob.setFilePath(relativePathString);

        List<JobWorkflowStep> jobSteps = new ArrayList<>();
        if (jobDTO.workflowSteps() != null) {
            jobSteps = createWorkflowSteps(jobDTO.workflowSteps(), savedJob);
        }

        //save workflow steps
        List<JobWorkflowStep> savedSteps = jobWfRepo.saveAll(jobSteps);
        savedJob.setWorkflowSteps(savedSteps);

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

            User wfStepProvider = userRepo.findByUid(stepDTO.providerUid())
                    .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
            jobWfStep.setProvider(wfStepProvider);

            User notifyUser = userRepo.findByUid(stepDTO.notifyUserUid())
                    .orElseThrow(() -> new ResourceNotFoundException("User to notify not found"));
            jobWfStep.setNotifyUser(notifyUser);

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

    public List<JobDTO> getJobsByProjectId(Long id) {
        List<Job> jobs = jobRepo.findByProjectId(id);
        return jobs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public FileDownloadDTO getJobFile(Long jobId, Long currentUserId) throws AccessDeniedException {
        Job job = jobRepo.findById(jobId)
        .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        if(!job.getJobOwner().getId().equals(currentUserId)){
            throw new AccessDeniedException("Not allowed to download this job file");
        }

        return new FileDownloadDTO(
            job.getFileName(), 
            job.getContentType(),
            job.getFilePath());
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

        return JobWorkflowStepDTO.from(wfStep);
    }

    public void deleteJob(Long id) {
        if (!jobRepo.existsById(id)){
            throw new ResourceNotFoundException("Job not found with id: " + id);
        }   
        
        jobRepo.deleteById(id);    
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
                job.getFilePath(),
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
}
