package com.tms.backend.job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.tms.backend.dto.FileDownloadDTO;
import com.tms.backend.dto.JobAnalyticsCountDTO;
import com.tms.backend.dto.JobDTO;
import com.tms.backend.dto.JobEditDTO;
import com.tms.backend.dto.JobSearchFilterByDate;
import com.tms.backend.dto.JobWorkflowStepDTO;
import com.tms.backend.exception.ResourceNotFoundException;
import com.tms.backend.project.Project;
import com.tms.backend.project.ProjectRepository;
import com.tms.backend.user.User;
import com.tms.backend.user.UserRepository;
import com.tms.backend.workflowSteps.WorkflowStep;
import com.tms.backend.workflowSteps.WorkflowStepRepository;

@Service
public class JobService {
    
    private final JobRepository jobRepo;
    private final ProjectRepository projectRepo;
    private final UserRepository userRepo;
    private final WorkflowStepRepository wfRepo;
    private final JobWorkflowStepRepository jobWfRepo;

    public JobService(JobRepository jobRepo, ProjectRepository projectRepo, UserRepository userRepo, WorkflowStepRepository wfRepo, JobWorkflowStepRepository jobWfRepo){
        this.jobRepo = jobRepo;
        this.projectRepo = projectRepo;
        this.userRepo = userRepo;
        this.wfRepo = wfRepo;
        this.jobWfRepo = jobWfRepo;
    }

    // @Transactional(readOnly = true)
    public List<JobDTO> getJobs() {
        List<Job> jobs = jobRepo.findAll();
        return jobs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public JobDTO findById(Long id, Long currentUserId) throws AccessDeniedException {
        Job job = jobRepo.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Job", "id", id));

        if (!job.getJobOwner().getId().equals(currentUserId)){
            throw new AccessDeniedException("Not allowed to view this project");
        }

        return convertToDTO(job);
    }

    public FileDownloadDTO downloadJobFile(Long jobId, Long currentUserId) throws AccessDeniedException {
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

    @Value("${file.upload-dir}")
    private String baseUploadDir;

    public Path getUserUploadPath(Long userId) {
        return Paths.get(baseUploadDir, String.valueOf(userId));
    }

    public JobDTO saveFileToLocal(MultipartFile file, JobDTO jobDTO) throws IOException {

        // set job details
        Job job = createJobFromDTO(jobDTO);

        // save to get the generated id
        Job savedJob = jobRepo.save(job);

        // add zero padding to ids
        String zeroPaddedProjId = String.format("%03d", jobDTO.projectId());
        String zeroPaddedJobId = String.format("%03d", savedJob.getId());

        // build folder name
        String projectFolder = "project-" + zeroPaddedProjId + "_" + LocalDateTime.now();
        String jobFolder = "job-" + zeroPaddedJobId + "_" + LocalDateTime.now();


        // Create folder structure: base/project/job
        Path uploadDirectory = Paths.get(baseUploadDir, projectFolder, jobFolder);

        // Create user folder if it doesn’t exist
        Files.createDirectories(uploadDirectory);

        // Resolve file path
        Path targetPath = uploadDirectory.resolve(file.getOriginalFilename());

        // Save file
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        savedJob.setFilePath(targetPath.toString());
        savedJob = jobRepo.save(savedJob);

        List<JobWorkflowStep> jobSteps = new ArrayList<>();
        if (jobDTO.workflowSteps() != null) {
            jobSteps = createWorkflowSteps(jobDTO.workflowSteps(), savedJob);
        }

        //save workflow steps
        List<JobWorkflowStep> savedSteps = jobWfRepo.saveAll(jobSteps);
        savedJob.setWorkflowSteps(savedSteps);

         return convertToDTO(savedJob);
    }

    private List<JobWorkflowStep> createWorkflowSteps(List<JobWorkflowStepDTO> stepDTOs, Job job) {
        List<JobWorkflowStep> jobSteps = new ArrayList<>();

        for (JobWorkflowStepDTO stepDTO : stepDTOs) {
            JobWorkflowStep jobWfStep = new JobWorkflowStep();
            jobWfStep.setJob(job);

            WorkflowStep wfStepReference = wfRepo.findById(stepDTO.workflowStepId())
                    .orElseThrow(() -> new ResourceNotFoundException("WorkflowStep not found"));
            jobWfStep.setWorkflowStep(wfStepReference);

            User wfStepProvider = userRepo.findById(stepDTO.providerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
            jobWfStep.setProvider(wfStepProvider);

            User notifyUser = userRepo.findById(stepDTO.notifyUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User to notify not found"));
            jobWfStep.setNotifyUser(notifyUser);

            jobWfStep.setDueDate(stepDTO.dueDate());
            jobWfStep.setStepOrder(stepDTO.stepOrder());

            jobSteps.add(jobWfStep);
        }

        return jobSteps;
    }

    private Job createJobFromDTO(JobDTO jobDTO) {
        Job job = new Job();
        job.setSourceLang(jobDTO.sourceLang());
        job.setTargetLangs(jobDTO.targetLangs());
        job.setStatus(jobDTO.status());
        job.setContentType(jobDTO.contentType());
        job.setDueDate(jobDTO.dueDate());
        job.setFileName(jobDTO.fileName());
        job.setFileSize(jobDTO.fileSize());
        if (jobDTO.progress() != null) {
            job.setProgress(jobDTO.progress().longValue());
        }
        job.setWordCount(jobDTO.wordCount());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userRepo.findByEmail(username)
        .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        job.setJobOwner(currentUser);

        if (jobDTO.projectId() != null) {
            Long projectId = jobDTO.projectId();
            Project project = projectRepo.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
            job.setProject(project);
        }

        if (jobDTO.providerId() != null) {
            User provider = userRepo.findById(jobDTO.providerId())
            .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
            job.setProvider(provider);
        }

        return job;
    }

    public JobEditDTO update(Long id, JobEditDTO updatedData){
        Job job = jobRepo.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));

        if (updatedData.providerId() != null){
            User provider = userRepo.findById(updatedData.providerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            job.setProvider(provider);
        }

        if (updatedData.status() != null) {
            job.setStatus(updatedData.status());
        }

        if (updatedData.dueDate() != null){
            job.setDueDate(updatedData.dueDate());
        }

        Job saved = jobRepo.save(job);
        return new JobEditDTO(
                saved.getProvider() != null ? saved.getProvider().getId() : null,
                saved.getStatus(),
                saved.getDueDate()
        );
    }

    public void deleteJob(Long id) {
        if (!jobRepo.existsById(id)){
            throw new ResourceNotFoundException("Project not found with id: " + id);
        }   
        
        jobRepo.deleteById(id);    
    }

    public JobDTO convertToDTO(Job job) {
         List<JobWorkflowStepDTO> stepDTOs = job.getWorkflowSteps().stream()
        .map(this::convertStepToDTO)  // Convert each entity to DTO
        .toList();

        return new JobDTO(
                job.getId(),
                job.getStatus(),
                job.getSourceLang(),
                job.getTargetLangs(),
                job.getProvider() != null ? job.getProvider().getId() : null,
                job.getDueDate(),
                job.getJobOwner() != null ? job.getJobOwner().getId() : null,
                job.getFileName(),
                job.getFileSize(),
                job.getFilePath(),
                job.getContentType(),
                job.getProject() != null ? job.getProject().getId() : null,
                stepDTOs,
                job.getWordCount(),
                job.getProgress(),
                job.getCreateDate()
        );
    }

    private JobWorkflowStepDTO convertStepToDTO(JobWorkflowStep step) {
    return new JobWorkflowStepDTO(
        step.getId(),
        step.getWorkflowStep().getId(),
        step.getWorkflowStep().getName(),
        step.getProvider() != null ? step.getProvider().getId() : null,
        step.getDueDate(),
        step.getNotifyUser() != null ? step.getNotifyUser().getId() : null,
        step.getWorkflowStatus(),
        step.getStepOrder()
        );
    }

    public JobAnalyticsCountDTO getJobCountByDate(JobSearchFilterByDate filter){
        // fallback if no filter is passed
        if (filter.fromDate() == null 
            && filter.toDate() == null 
            && filter.year() == null 
            && filter.month() == null 
            && filter.period() == null) {

        return jobRepo.getDeliveryByMonthCount(
            LocalDateTime.of(1970, 1, 1, 0, 0),
            LocalDateTime.of(9999, 12, 31, 23, 59, 59)
        );
        }

        LocalDate fromDate = filter.fromDate();
        LocalDate toDate = filter.toDate();


        // derive date range from year/month
        if (filter.year() != null && filter.month() != null) {
            YearMonth ym = YearMonth.of(filter.year(), filter.month());
            fromDate = ym.atDay(1);
            toDate = ym.atEndOfMonth();
        } else if (filter.year() != null) {
            fromDate = LocalDate.of(filter.year(), 1, 1);
            toDate = LocalDate.of(filter.year(), 12, 31);
        }

        // derive from semantic period
        if (filter.period() != null) {
            LocalDate today = LocalDate.now();
            switch (filter.period().toUpperCase()) {
                case "THIS_MONTH" -> {
                    YearMonth ym = YearMonth.from(today);
                    fromDate = ym.atDay(1);
                    toDate = ym.atEndOfMonth();
                }
                case "THIS_YEAR" -> {
                    fromDate = LocalDate.of(today.getYear(), 1, 1);
                    toDate = LocalDate.of(today.getYear(), 12, 31);
                }
                case "THIS_WEEK" -> {
                    DayOfWeek firstDayOfWeek = DayOfWeek.MONDAY; // or SUNDAY, depending on business rules
                    fromDate = today.with(firstDayOfWeek);
                    toDate = fromDate.plusDays(6);
                }
            }
        }

        // Convert LocalDate to LocalDateTime
        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate.atTime(23, 59, 59);

        return jobRepo.getDeliveryByMonthCount(fromDateTime, toDateTime);
    }
}
