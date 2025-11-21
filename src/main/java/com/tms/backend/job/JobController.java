package com.tms.backend.job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.tms.backend.dto.DownloadJobsRequest;
import com.tms.backend.dto.DownloadProjectsRequest;
import com.tms.backend.dto.JobDTO;
import com.tms.backend.dto.JobSoftDeleteDTO;
import com.tms.backend.dto.JobWorkflowStepDTO;
import com.tms.backend.dto.JobWorkflowStepEditDTO;
import com.tms.backend.dto.ProjectWithJobDTO;
import com.tms.backend.dto.TranslatedFileUploadRequest;
import com.tms.backend.exception.ResourceNotFoundException;
import com.tms.backend.user.CustomUserDetails;

import jakarta.persistence.EntityNotFoundException;

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

    @PostMapping("/upload-translated")
    public ResponseEntity<?> uploadTranslatedFile(@ModelAttribute TranslatedFileUploadRequest request) {
        try {
            logger.info("Uploading translated file for job ID: {}", request.getJobId());

            // Validate jobId
            if (request.getJobId() == null) {
                return ResponseEntity.badRequest().body("Job ID is required");
            }

            // Validate file
            if (request.getFile() == null || request.getFile().isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            // Validate XLIFF extension
            String filename = request.getFile().getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".xliff")) {
                return ResponseEntity.badRequest().body("File must be an XLIFF file");
            }

            Path savedPath = jobService.uploadTranslatedFile(request.getFile(), request.getJobId());

            return ResponseEntity.ok()
                    .body(Map.of(
                            "message", "Translated file uploaded successfully",
                            "filePath", savedPath.toString()));

        } catch (EntityNotFoundException e) {
            logger.error("Job not found: {}", request.getJobId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Job not found with ID: " + request.getJobId());
        } catch (IOException e) {
            logger.error("Failed to upload translated file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload file: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred");
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<JobDTO> getJobById(@PathVariable Long id) {
        try {
            JobDTO job = jobService.getJobDTOById(id);
            return ResponseEntity.ok(job);
        } catch (ResourceNotFoundException e) {
            logger.error("Job not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{jobId}/workflow-step")
    public ResponseEntity<JobWorkflowStepDTO> updateWorkflowSteps(
        @PathVariable Long jobId, 
        @RequestBody JobWorkflowStepEditDTO stepUpdate,
        Authentication authentication) 
    {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String uid = userDetails.getUid();
        
        JobWorkflowStepDTO updatedWf = jobService.updateWorkflowStep(jobId, stepUpdate, uid);
        return ResponseEntity.ok(updatedWf);
    }

    @DeleteMapping("/{id}/hard")
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

    // Download by PROJECT IDs
    @PostMapping("/download/converted/projects")
    public ResponseEntity<StreamingResponseBody> downloadByProjects(@RequestBody DownloadProjectsRequest request) {
        if (request.getProjectIds() == null || request.getProjectIds().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (request.getProjectIds().size() == 1) {
            return downloadSingleProjectFiles(request.getProjectIds().get(0));
        }

        return downloadMultipleProjectsFiles(request.getProjectIds());
    }

    private ResponseEntity<StreamingResponseBody> downloadSingleProjectFiles(Long projectId) {
        // Get all jobs for this project first
        List<Job> jobs = jobService.getJobEntitiesByProjectId(projectId);

        if (jobs.isEmpty()) {
            logger.warn("No jobs found for project {}", projectId);
            return ResponseEntity.notFound().build();
        }

        // Get project name before creating the stream (must be effectively final for
        // lambda)
        String projectName = "project"; // Default fallback
        try {
            projectName = jobs.get(0).getProject().getName();
            projectName = sanitizeFolderName(projectName);
        } catch (Exception e) {
            logger.error("Error getting project name, using default", e);
        }

        // Make it effectively final for use in lambda
        final String finalProjectName = projectName;

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String zipFileName = finalProjectName + "-" + timestamp + ".zip";

        StreamingResponseBody stream = outputStream -> {
            try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
                logger.info("Processing {} jobs for project: {}", jobs.size(), finalProjectName);

                for (Job job : jobs) {
                    try {
                        Path filePath = jobService.getConvertedFilePath(job.getId());

                        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                            logger.warn("Skipping job {}: file not found or not readable", job.getId());
                            continue;
                        }

                        // Add file to ZIP root (no subfolder for single project)
                        String zipEntryName = "job" + job.getId() + "_" + job.getConvertedFileName();
                        ZipEntry zipEntry = new ZipEntry(zipEntryName);
                        zipOut.putNextEntry(zipEntry);

                        long bytesCopied = Files.copy(filePath, zipOut);
                        zipOut.closeEntry();

                        logger.info("Added {} bytes for job {}", bytesCopied, job.getId());

                    } catch (Exception e) {
                        logger.error("Error adding job {}: {}", job.getId(), e.getMessage());
                    }
                }

                logger.info("Completed ZIP for project: {}", finalProjectName);
            }
        };
    
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + zipFileName + "\"")
                .body(stream);
    }

    // Download multiple projects
    private ResponseEntity<StreamingResponseBody> downloadMultipleProjectsFiles(List<Long> projectIds) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String zipFileName = "projects-" + timestamp + ".zip";

        StreamingResponseBody stream = outputStream -> {
            try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
                logger.info("Processing {} projects", projectIds.size());

                for (Long projectId : projectIds) {
                    try {
                        // Get all jobs for this project
                        List<Job> jobs = jobService.getJobEntitiesByProjectId(projectId);

                        if (jobs.isEmpty()) {
                            logger.warn("No jobs found for project {}", projectId);
                            continue;
                        }

                        String projectName = jobs.get(0).getProject().getName();
                        // Sanitize project name for folder name (remove special characters)
                        String folderName = sanitizeFolderName(projectName);

                        logger.info("Processing project: {} ({} jobs)", projectName, jobs.size());

                        // Add each job's file to the project folder
                        for (Job job : jobs) {
                            try {
                                Path filePath = jobService.getConvertedFilePath(job.getId());

                                if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                                    logger.warn("Skipping job {}: file not found", job.getId());
                                    continue;
                                }

                                // Organize: ProjectName/job123_filename.xml
                                String zipEntryName = folderName + "/job" + job.getId() + "_"
                                        + job.getConvertedFileName();
                                ZipEntry zipEntry = new ZipEntry(zipEntryName);
                                zipOut.putNextEntry(zipEntry);

                                long bytesCopied = Files.copy(filePath, zipOut);
                                zipOut.closeEntry();

                                logger.info("Added {} bytes for job {} in project {}",
                                        bytesCopied, job.getId(), projectName);

                            } catch (Exception e) {
                                logger.error("Error adding job {} in project {}: {}",
                                        job.getId(), projectName, e.getMessage());
                            }
                        }

                    } catch (Exception e) {
                        logger.error("Error processing project {}: {}", projectId, e.getMessage());
                    }
                }

                logger.info("Completed multi-project ZIP");
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + zipFileName + "\"")
                .body(stream);
    }

    // Download by JOB IDs
    @PostMapping("/download/converted/jobs")
    public ResponseEntity<StreamingResponseBody> downloadByJobs(@RequestBody DownloadJobsRequest request) {
        if (request.getJobIds() == null || request.getJobIds().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (request.getJobIds().size() == 1) {
            return downloadSingleFile(request.getJobIds().get(0));
        }

        return downloadMultipleJobsAsZip(request.getJobIds());
    }

    // @PostMapping("/download/converted")
    // public ResponseEntity<StreamingResponseBody> downloadConvertedFiles(@RequestBody DownloadJobsRequest request) {
    //     if (request.getJobIds() == null || request.getJobIds().isEmpty()) {
    //         return ResponseEntity.badRequest().build();
    //     }

    //     if (request.getJobIds().size() == 1) {
    //         return downloadSingleFile(request.getJobIds().get(0));
    //     }

    //     return downloadMultipleFilesAsZip(request.getJobIds());
    // }

    private ResponseEntity<StreamingResponseBody> downloadSingleFile(Long jobId) {
        try {
            Job job = jobService.getJobById(jobId);
            Path filePath = jobService.getConvertedFilePath(jobId);

            StreamingResponseBody stream = outputStream -> {
                Files.copy(filePath, outputStream);
            };

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/xml"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + job.getConvertedFileName() + "\"")
                    .body(stream);
        } catch (Exception e) {
            logger.error("Error downloading file for job {}", jobId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<StreamingResponseBody> downloadMultipleJobsAsZip(List<Long> jobIds) {

        String projectName = "project"; // Default fallback
    
        try {
            if (!jobIds.isEmpty()) {
                // Get project name from the first job
                Job firstJob = jobService.getJobById(jobIds.get(0));
                projectName = firstJob.getProject().getName();
            }
        } catch (Exception e) {
            logger.error("Error getting project name, using default", e);
        }

        StreamingResponseBody stream = outputStream -> {
            try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
                for (Long jobId : jobIds) {
                    try {
                        Job job = jobService.getJobById(jobId);
                        Path filePath = jobService.getConvertedFilePath(jobId);

                        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                            continue;
                        }

                        String zipEntryName = "job" + jobId + "_" + job.getConvertedFileName();
                        ZipEntry zipEntry = new ZipEntry(zipEntryName);
                        zipOut.putNextEntry(zipEntry);
                        Files.copy(filePath, zipOut);
                        zipOut.closeEntry();
                    } catch (Exception e) {
                        logger.warn("Skipping file for job {}", jobId);
                    }
                }
            }
        };

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String zipFileName = projectName + "-" + timestamp + ".zip";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + zipFileName + ".zip\"")
                .body(stream);
    }

    private String sanitizeFolderName(String name) {
        if (name == null || name.isEmpty()) {
            return "unnamed_project";
        }
        // Remove or replace special characters that aren't allowed in file paths
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
