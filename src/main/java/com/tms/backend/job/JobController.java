package com.tms.backend.job;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tms.backend.dto.JobDTO;
import com.tms.backend.dto.JobSoftDeleteDTO;
import com.tms.backend.dto.JobWorkflowStepDTO;
import com.tms.backend.dto.JobWorkflowStepEditDTO;
import com.tms.backend.dto.ProjectWithJobDTO;
import com.tms.backend.exception.ResourceNotFoundException;
import com.tms.backend.user.CustomUserDetails;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;


@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    private static final Logger logger = LoggerFactory.getLogger(JobController.class);


    public JobController(JobService jobService){
        this.jobService = jobService;
    }
    
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> createJob(
        @RequestPart("file") MultipartFile file,
        @RequestPart("job") JobDTO jobDTO,
        Authentication authentication) 
    {
            try {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                String uid = userDetails.getUid();

                JobDTO savedJob = jobService.createJob(file, jobDTO, uid);
                Map<String, Object> response = new HashMap<>();
                response.put("message", "File uploaded successfully");
                response.put("job", savedJob);
                
                return ResponseEntity.ok(response);
            } catch (IOException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Error uploading file: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
            }
    }

    @PostMapping("/uploadFromPortal")
    public ResponseEntity<ProjectWithJobDTO> createMultipleJobs(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("projectNote") String note,
            @RequestPart("job") JobDTO jobDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {

        String uid = userDetails.getUid(); // Extract uid from CustomUserDetails

        ProjectWithJobDTO results = jobService.createProjectWithJobs(files, note, jobDTO, uid);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<JobDTO> getAllJobs() {
        return jobService.getJobs();
    }

    @PutMapping("/{jobId}/workflow-step/{stepId}")
    public ResponseEntity<JobWorkflowStepDTO> updateWorkflowSteps(@PathVariable Long jobId, @PathVariable Long stepId, @RequestBody JobWorkflowStepEditDTO stepUpdate) {
        JobWorkflowStepDTO updatedWf = jobService.updateWorkflowStep(jobId, stepUpdate);
        return ResponseEntity.ok(updatedWf);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteJob(@PathVariable Long id){
        try {
            jobService.deleteJob(id);
            return ResponseEntity.ok("Job deleted successfully");
        } catch (IOException e) {
            // Log and return an appropriate error response
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting job files: " + e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    /**
     * Soft delete a single job
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDeleteJob(
            @PathVariable Long id,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String uid = userDetails.getUid();
        
        jobService.softDeleteJob(id, uid);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restore a soft deleted job
     */
    @PatchMapping("/{id}/restore")
    public ResponseEntity<JobDTO> restoreJob(
            @PathVariable Long id,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String uid = userDetails.getUid();
        
        JobDTO restored = jobService.restoreJob(id, uid);
        return ResponseEntity.ok(restored);
    }

    /**
     * Get deleted jobs (for recycle bin)
     */
    @GetMapping("/deleted")
    public ResponseEntity<List<JobSoftDeleteDTO>> getSoftDeletedJobs(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String uid = userDetails.getUid();
        
        List<JobSoftDeleteDTO> deletedJobs = jobService.getDeletedJobsByUser(uid);
        return ResponseEntity.ok(deletedJobs);
    }

    // Download original file
    @GetMapping("/{jobId}/download/original")
    public ResponseEntity<Resource> downloadOriginalFile(@PathVariable Long jobId) {
        try {
            Job job = jobService.getJobById(jobId);
            Path filePath = jobService.getOriginalFilePath(jobId);
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                logger.error("Original file not readable: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
            String contentType = job.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            logger.info("Downloading original file for job {}: {}", jobId, job.getOriginalFileName());
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + job.getOriginalFileName() + "\"")
                .body(resource);
                
        } catch (ResourceNotFoundException e) {
            logger.error("File not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error downloading original file for job: " + jobId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Download converted file
    @GetMapping("/{jobId}/download/converted")
    public ResponseEntity<Resource> downloadConvertedFile(@PathVariable Long jobId) {
        try {
            Job job = jobService.getJobById(jobId);
            Path filePath = jobService.getConvertedFilePath(jobId);
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                logger.error("Converted file not readable: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
            logger.info("Downloading converted file for job {}: {}", jobId, job.getConvertedFileName());
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/xml"))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + job.getConvertedFileName() + "\"")
                .body(resource);
                
        } catch (ResourceNotFoundException e) {
            logger.error("File not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error downloading converted file for job: " + jobId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
