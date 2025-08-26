package com.tms.backend.job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import com.tms.backend.dto.FileDownloadDTO;
import com.tms.backend.dto.JobDTO;
import com.tms.backend.dto.JobEditDTO;
import com.tms.backend.exception.ResourceNotFoundException;
import com.tms.backend.project.Project;
import com.tms.backend.project.ProjectRepository;
import com.tms.backend.user.User;
import com.tms.backend.user.UserRepository;

// @Service
public class JobService {
    
    private final JobRepository repo;
    private final ProjectRepository projectRepo;
    private final UserRepository userRepo;

    public JobService(JobRepository repo, ProjectRepository projectRepo, UserRepository userRepo){
        this.repo = repo;
        this.projectRepo = projectRepo;
        this.userRepo = userRepo;    
    }

    public List<JobDTO> getAllJobs() {
        return repo.findAll()
        .stream()
        .map(this::convertToDTO)
        .collect(Collectors.toList());
    }

    public JobDTO findById(Long id, Long currentUserId) throws AccessDeniedException {
        Job job = repo.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Job", "id", id));

        if (!job.getJobOwner().getId().equals(currentUserId)){
            throw new AccessDeniedException("Not allowed to view this project");
        }

        return convertToDTO(job);
    }

    public FileDownloadDTO downloadJobFile(Long jobId, Long currentUserId) throws AccessDeniedException {
        Job job = repo.findById(jobId)
        .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        if(!job.getJobOwner().getId().equals(currentUserId)){
            throw new AccessDeniedException("Not allowed to download this job file");
        }

        return new FileDownloadDTO(
            job.getFileName(), 
            job.getContentType(),
            job.getData());
    }

    public Job saveFile(MultipartFile file, String status, Set<String> targetLangs, String provider, LocalDateTime dueDate, Long projectId) throws IOException {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepo.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        //Read file content
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        byte[] data = file.getBytes();

        Job job = new Job();
        job.setJobOwner(user);
        job.setFileName(filename);
        job.setContentType(contentType);
        job.setData(data);

        // job.setConfirmPct(null);
        job.setStatus(status);
        job.setTargetLangs(targetLangs);
        job.setProvider(provider);
        job.setDueDate(dueDate);

        //associate with project
        if(projectId != null){
            Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
            job.setProject(project);
        }
        
        return repo.save(job);
    }

    @Value("${file.upload-dir}")
    private String baseUploadDir;

    public Path getUserUploadPath(Long userId) {
        return Paths.get(baseUploadDir, String.valueOf(userId));
    }

    public void saveFile2(MultipartFile file) throws IOException {
        // Path userDir = getUserUploadPath(userId);

        Path userDir = Paths.get(baseUploadDir);

        // Create user folder if it doesn’t exist
        Files.createDirectories(userDir);

        // Resolve file path
        Path targetPath = userDir.resolve(file.getOriginalFilename());

        // Save file
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public JobEditDTO update(Long id, JobEditDTO updatedData){
        Job job = repo.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));

        if (updatedData.provider() != null){
            job.setProvider(updatedData.provider());
        }
        if (updatedData.status() != null){
            job.setStatus(updatedData.status());
        }

        if (updatedData.dueDate() != null){
            job.setDueDate(updatedData.dueDate());
        }

        Job saved = repo.save(job);
        return new JobEditDTO(
            saved.getProvider(),
            saved.getStatus(),
            saved.getDueDate()
        );
    }

    public void deleteJob(Long id) {
        if (!repo.existsById(id)){
            throw new ResourceNotFoundException("Project not found with id: " + id);
        }   
        
        repo.deleteById(id);    
    }

    public JobDTO convertToDTO(Job job) {
        return new JobDTO(
                job.getId(),
                job.getStatus(),
                job.getTargetLangs(),
                job.getProvider(),
                job.getDueDate(),
                job.getJobOwner() != null ? job.getJobOwner().getId() : null,
                job.getFileName(),
                job.getContentType(),
                job.getProject() != null ? job.getProject().getId() : null
        );
    }
}
