package com.tms.backend.job;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.tms.backend.dto.DownloadJobsRequest;
import com.tms.backend.dto.DownloadProjectsRequest;
import com.tms.backend.dto.JobCheckoutStatusDTO;
import com.tms.backend.dto.JobDTO;
import com.tms.backend.dto.JobRowDTO;
import com.tms.backend.dto.JobSoftDeleteDTO;
import com.tms.backend.dto.JobWorkflowStepDTO;
import com.tms.backend.dto.JobWorkflowStepEditDTO;
import com.tms.backend.dto.JobWorkflowStepStatusUpdateDTO;
import com.tms.backend.dto.PagedResponseDTO;
import com.tms.backend.dto.ProjectWithJobDTO;
import com.tms.backend.dto.TranslatedFileUploadRequest;
import com.tms.backend.exception.ResourceNotFoundException;
import com.tms.backend.settingCompletedFilesNaming.CompletedFilesNamingSetting;
import com.tms.backend.settingCompletedFilesNaming.CompletedFilesNamingSettingService;
import com.tms.backend.tomato.FileConversionService;
import com.tms.backend.user.CustomUserDetails;
import com.tms.backend.user.User;
import com.tms.backend.user.UserService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;


@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final FileConversionService fileService;
    private final UserService userService;
    private final CompletedFilesNamingSettingService completedFilesNamingSettingService;

    private static final Logger logger = LoggerFactory.getLogger(JobController.class);

    @Value("${file.upload-dir}")
    private String baseUploadDir;

    public JobController(
        JobService jobService,
        FileConversionService fileService,
        UserService userService,
        CompletedFilesNamingSettingService completedFilesNamingSettingService){
        this.jobService = jobService;
        this.fileService = fileService;
        this.userService = userService;
        this.completedFilesNamingSettingService = completedFilesNamingSettingService;
    }

@PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> createJob(
        @RequestPart("file") MultipartFile file,
        @RequestPart("job") JobDTO jobDTO,
        @RequestParam(required = false, defaultValue = "false") Boolean performSizingDuringCreation,
        Authentication authentication)
    {
            try {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                String uid = userDetails.getUid();

                List<JobDTO> savedJobs = jobService.createJob(file, jobDTO, uid, null, false, performSizingDuringCreation);
                Map<String, Object> response = new HashMap<>();
                response.put("message", "File uploaded successfully");
                // One upload can produce multiple jobs (one per target language)
                response.put("jobs", savedJobs);

                return ResponseEntity.ok(response);
            } catch (IOException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Error uploading file: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
            }
    }

    @PostMapping("/batch")
    public ResponseEntity<?> createJobs(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("job") JobDTO jobDTO,
            @RequestPart(value = "note", required = false) String note,
            @RequestParam(required = false, defaultValue = "false") Boolean performSizingDuringCreation,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {

        String uid = userDetails.getUid();

        if (jobDTO.projectId() != null) {
            // Jobs under an existing project
            List<JobDTO> jobs = jobService.createJobs(files, jobDTO, uid, null, false, performSizingDuringCreation);
            return ResponseEntity.ok(jobs);
        } else {
            // No project provided, auto-create one and attach the jobs to it
            ProjectWithJobDTO result = jobService.createProjectWithJobs(files, note != null ? note : "", jobDTO, uid);
            return ResponseEntity.ok(result);
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
    public List<JobDTO> getAllJobs(@AuthenticationPrincipal CustomUserDetails userDetails) {
        // Get current user
        String uid = userDetails.getUid();
        User currentUser = userService.findByUid(uid)
            .orElseThrow(() -> new RuntimeException("User not found with uid: " + uid));
        return jobService.getJobs(currentUser);
    }

    // Flattened (one row per workflow step), paginated/filtered/sorted/searchable job list.
    // Same role/owner scoping as getAllJobs above, applied over the expanded row set.
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public PagedResponseDTO<JobRowDTO> getJobRows(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String filters) {
        String uid = userDetails.getUid();
        User currentUser = userService.findByUid(uid)
            .orElseThrow(() -> new RuntimeException("User not found with uid: " + uid));

        Map<String, List<String>> parsedFilters = parseFilters(filters);
        return PagedResponseDTO.from(
                jobService.getJobRows(currentUser, page, pageSize, search, sortBy, sortDir, parsedFilters));
    }

    private Map<String, List<String>> parseFilters(String filtersJson) {
        if (filtersJson == null || filtersJson.isBlank()) {
            return null;
        }
        try {
            return new ObjectMapper().readValue(filtersJson, new TypeReference<Map<String, List<String>>>() {});
        } catch (JsonProcessingException e) {
            return null;
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

    @GetMapping("/workflow-step-statuses")
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, String>> getWorkflowStepStatuses() {
        return java.util.Arrays.stream(JobWorkflowStatus.values())
            .map(s -> Map.of("value", s.name(), "label", s.getName()))
            .collect(java.util.stream.Collectors.toList());
    }

    @PatchMapping("/{jobId}/workflow-steps/{stepId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JobWorkflowStepDTO> updateWorkflowStepStatus(
        @PathVariable Long jobId,
        @PathVariable Long stepId,
        @RequestBody JobWorkflowStepStatusUpdateDTO body,
        @AuthenticationPrincipal CustomUserDetails userDetails)
    {
        String uid = userDetails.getUid();
        JobWorkflowStepDTO updated = jobService.updateWorkflowStepStatus(jobId, stepId, body.status(), uid);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/checkout-status")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<JobCheckoutStatusDTO> getCheckoutStatus(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(jobService.getCheckoutStatus(id));
        } catch (ResourceNotFoundException e) {
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

    
    // Restore a soft deleted job
    @PatchMapping("/{id}/restore")
    public ResponseEntity<JobDTO> restoreJob(
            @PathVariable Long id,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String uid = userDetails.getUid();
        
        JobDTO restored = jobService.restoreJob(id, uid);
        return ResponseEntity.ok(restored);
    }

    
    // Get deleted jobs (for recycle bin)
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

    // Download translated file
    @GetMapping("/{jobId}/download/translated")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadTranslatedFile(
            @PathVariable Long jobId,
            @RequestParam(required = false) Long workflowStepId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Job job = jobService.getJobById(jobId);
            Path filePath = jobService.getTranslatedFilePath(jobId);

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                logger.error("Translated file not readable: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            String downloadFileName = job.getTranslatedFileName();

            if (userDetails != null && isJobCompleted(job, workflowStepId)) {
                User currentUser = userService.findByUid(userDetails.getUid()).orElse(null);
                if (currentUser != null) {
                    CompletedFilesNamingSetting setting = completedFilesNamingSettingService.getForUser(currentUser);
                    if (setting.isHasNamingRule()) {
                        downloadFileName = applyNamingRule(setting.getNamingRule(), job);
                    }
                }
            }

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/xml"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + downloadFileName + "\"")
                .body(resource);

        } catch (ResourceNotFoundException e) {
            logger.error("File not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error downloading translated file for job: " + jobId, e);
            return ResponseEntity.internalServerError().build();
        }
    }


    // Download translated files for multiple jobs at once
    @PostMapping("/download/translated/multiple")
    public ResponseEntity<?> downloadTranslatedByJobs(@RequestBody DownloadJobsRequest request) {
        if (request.getJobIds() == null || request.getJobIds().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (request.getJobIds().size() == 1) {
            return downloadSingleTranslatedFile(request.getJobIds().get(0));
        }

        return downloadMultipleTranslatedJobsAsZip(request.getJobIds());
    }

    private ResponseEntity<StreamingResponseBody> downloadSingleTranslatedFile(Long jobId) {
        try {
            Job job = jobService.getJobById(jobId);
            Path filePath = jobService.getTranslatedFilePath(jobId);

            if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                logger.error("Translated file not readable: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            StreamingResponseBody stream = outputStream -> {
                Files.copy(filePath, outputStream);
            };

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/xml"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + job.getTranslatedFileName() + "\"")
                    .body(stream);
        } catch (Exception e) {
            logger.error("Error downloading translated file for job {}", jobId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<?> downloadMultipleTranslatedJobsAsZip(List<Long> jobIds) {

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

        final String finalProjectName = sanitizeFolderName(projectName);

        // Resolve every job's file up front so we know which ones will be skipped
        // before the zip is built.
        List<ResolvedZipEntry> resolved = new ArrayList<>();
        List<SkippedJobInfo> skipped = new ArrayList<>();

        for (Long jobId : jobIds) {
            Job job = null;
            try {
                job = jobService.getJobById(jobId);
                Path filePath = jobService.getTranslatedFilePath(jobId);

                if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                    logger.warn("Skipping job {}: translated file not found or not readable", jobId);
                    skipped.add(new SkippedJobInfo(jobId, describeJob(job), "file not found"));
                    continue;
                }

                String zipEntryName = groupFolderName(job) + "/" + appendTargetLang(job.getTranslatedFileName(), job);
                resolved.add(new ResolvedZipEntry(filePath, zipEntryName));
            } catch (Exception e) {
                logger.warn("Skipping translated file for job {}: {}", jobId, e.getMessage());
                String label = job != null ? describeJob(job) : "Job " + jobId;
                skipped.add(new SkippedJobInfo(jobId, label, sanitizeHeaderValue(e.getMessage())));
            }
        }

        try {
            return buildZipDownloadResponse(finalProjectName + "-translated.zip", resolved, skipped);
        } catch (IOException e) {
            logger.error("Error building translated zip for jobs {}", jobIds, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private boolean isJobCompleted(Job job, Long workflowStepId) {
        if (workflowStepId == null) {
            return false;
        }
        Set<JobWorkflowStep> steps = job.getWorkflowSteps();
        return steps.stream()
                .anyMatch(ws -> ws.getId().equals(workflowStepId)
                        && ws.getStatus() == JobWorkflowStatus.COMPLETED);
    }

    private String applyNamingRule(String rule, Job job) {
        String translatedFileName = job.getTranslatedFileName() != null
                ? job.getTranslatedFileName() : job.getFileName();

        String baseName = translatedFileName;
        String extension = "";
        int dotIndex = translatedFileName.lastIndexOf('.');
        if (dotIndex != -1) {
            baseName = translatedFileName.substring(0, dotIndex);
            extension = translatedFileName.substring(dotIndex);
        }

        String sourceLang = job.getSourceLang() != null ? job.getSourceLang() : "";
        String targetLang = (job.getTargetLangs() != null && !job.getTargetLangs().isEmpty())
                ? job.getTargetLangs().stream().sorted().collect(Collectors.joining("-"))
                : "";
        String workflow = job.getWorkflowSteps().stream()
                .filter(ws -> ws.getWorkflowStep() != null && ws.getWorkflowStep().getName() != null)
                .map(ws -> ws.getWorkflowStep().getName())
                .collect(Collectors.joining("-"));

        String result = rule
                .replace("{path}", "")
                .replace("{fileName}", baseName)
                .replace("{sourceLang}", sourceLang)
                .replace("{targetLang}", targetLang)
                .replace("{workflow}", workflow)
                .replace("{status}", "completed");

        // Strip any path prefix — only use the filename segment
        int slashIndex = result.lastIndexOf('/');
        if (slashIndex != -1) {
            result = result.substring(slashIndex + 1);
        }

        // Clean up repeated or leading/trailing dashes
        result = result.replaceAll("-{2,}", "-").replaceAll("^[-_]+|[-_]+$", "");

        return result + extension;
    }

    @PostMapping("/{jobId}/save-xliff-from-sizing")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> saveXliffFromSizing(
            @PathVariable Long jobId,
            @RequestParam String tomatoJobId) {
        try {
            JobDTO updated = jobService.saveXliffFromSizing(jobId, tomatoJobId);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to save XLIFF from sizing for job {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save XLIFF: " + e.getMessage());
        }
    }

    @PostMapping("/{jobId}/download/target")
    public ResponseEntity<Resource> downloadTargetFile(@PathVariable Long jobId) throws IOException {

        // delegate to service
        Path relativeTargetPath = jobService.generateTargetFile(jobId);

        Path absolutePath = Paths.get(baseUploadDir).resolve(relativeTargetPath);
        Resource resource = new UrlResource(absolutePath.toUri());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + absolutePath.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    // Download target files for multiple jobs at once
    @PostMapping("/download/target/multiple")
    public ResponseEntity<?> downloadTargetFilesByJobs(@RequestBody DownloadJobsRequest request) {
        if (request.getJobIds() == null || request.getJobIds().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (request.getJobIds().size() == 1) {
            return downloadSingleTargetFile(request.getJobIds().get(0));
        }

        return downloadMultipleTargetFilesAsZip(request.getJobIds());
    }

    private ResponseEntity<StreamingResponseBody> downloadSingleTargetFile(Long jobId) {
        try {
            Path relativeTargetPath = jobService.generateTargetFile(jobId);
            Path absolutePath = Paths.get(baseUploadDir).resolve(relativeTargetPath);

            StreamingResponseBody stream = outputStream -> {
                Files.copy(absolutePath, outputStream);
            };

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + absolutePath.getFileName() + "\"")
                    .body(stream);
        } catch (Exception e) {
            logger.error("Error downloading target file for job {}", jobId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<?> downloadMultipleTargetFilesAsZip(List<Long> jobIds) {

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

        final String finalProjectName = sanitizeFolderName(projectName);

        // Resolve every job's file up front so we know which ones will be skipped
        // before the zip is built.
        List<ResolvedZipEntry> resolved = new ArrayList<>();
        List<SkippedJobInfo> skipped = new ArrayList<>();

        for (Long jobId : jobIds) {
            Job job = null;
            try {
                job = jobService.getJobById(jobId);
                Path relativeTargetPath = jobService.generateTargetFile(jobId);
                Path absolutePath = Paths.get(baseUploadDir).resolve(relativeTargetPath);

                if (!Files.exists(absolutePath) || !Files.isReadable(absolutePath)) {
                    logger.warn("Skipping job {}: target file not found or not readable", jobId);
                    skipped.add(new SkippedJobInfo(jobId, describeJob(job), "file not found"));
                    continue;
                }

                String zipEntryName = groupFolderName(job) + "/" + appendTargetLang(absolutePath.getFileName().toString(), job);
                resolved.add(new ResolvedZipEntry(absolutePath, zipEntryName));
            } catch (Exception e) {
                logger.warn("Skipping target file for job {}: {}", jobId, e.getMessage());
                String label = job != null ? describeJob(job) : "Job " + jobId;
                skipped.add(new SkippedJobInfo(jobId, label, sanitizeHeaderValue(e.getMessage())));
            }
        }

        try {
            return buildZipDownloadResponse(finalProjectName + "-target.zip", resolved, skipped);
        } catch (IOException e) {
            logger.error("Error building target zip for jobs {}", jobIds, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Download by project IDs
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

    // Download by job IDs
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

    // A file resolved and ready to be written into the zip: its source path on disk
    // and the entry name it should be written under.
    private record ResolvedZipEntry(Path path, String zipEntryName) {
    }

    // A job whose file could not be added to the zip, and why. `label` is the
    // human-readable "fileName (sourceLang-targetLang)" identifier shown in the manifest.
    private record SkippedJobInfo(Long jobId, String label, String reason) {
    }

    // "fileName.ext (EN-KO)" identifier for a job, used anywhere a skipped file needs to be
    // described to a human without making them look up the job id.
    private String describeJob(Job job) {
        String fileName = job.getFileName() != null ? job.getFileName() : "job" + job.getId();
        String sourceLang = job.getSourceLang() != null ? job.getSourceLang() : "";
        String targetLang = (job.getTargetLangs() != null && !job.getTargetLangs().isEmpty())
                ? job.getTargetLangs().stream().sorted().collect(Collectors.joining("-"))
                : "";
        String langPair = !sourceLang.isEmpty() && !targetLang.isEmpty()
                ? " (" + sourceLang + "-" + targetLang + ")" : "";
        return fileName + langPair;
    }

    // Trims control characters and caps length on a raw exception message before it's
    // embedded in a JSON response or the in-zip manifest.
    private String sanitizeHeaderValue(String value) {
        if (value == null || value.isBlank()) {
            return "error";
        }
        String cleaned = value.replaceAll("[\\r\\n]", " ").trim();
        return cleaned.length() > 100 ? cleaned.substring(0, 100) : cleaned;
    }

    // Human-readable manifest written into the zip itself (as "_skipped_files.txt") as a
    // secondary record of what was omitted, in case someone opens the archive directly.
    private byte[] buildSkippedFilesManifest(List<SkippedJobInfo> skipped) {
        String header = "The following " + skipped.size() + " file(s) could not be included in this archive:\n\n";
        String body = skipped.stream()
                .map(s -> s.label() + ": " + s.reason())
                .collect(Collectors.joining("\n"));
        return (header + body + "\n").getBytes(StandardCharsets.UTF_8);
    }

    // Builds the zip in memory and returns it as a JSON body ({fileName, data: base64,
    // skippedJobs}) rather than a raw octet-stream. The skipped-file list needs to reach the
    // frontend reliably in every deployment (including cross-origin ones), and a JSON body is
    // always readable by fetch() regardless of CORS exposedHeaders config - unlike a custom
    // response header, which silently disappears cross-origin unless explicitly exposed.
    private ResponseEntity<Map<String, Object>> buildZipDownloadResponse(
            String zipFileName, List<ResolvedZipEntry> resolved, List<SkippedJobInfo> skipped) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
            for (ResolvedZipEntry entry : resolved) {
                try {
                    zipOut.putNextEntry(new ZipEntry(entry.zipEntryName()));
                    Files.copy(entry.path(), zipOut);
                    zipOut.closeEntry();
                } catch (Exception e) {
                    logger.warn("Failed to add {} to zip: {}", entry.zipEntryName(), e.getMessage());
                }
            }

            if (!skipped.isEmpty()) {
                zipOut.putNextEntry(new ZipEntry("_skipped_files.txt"));
                zipOut.write(buildSkippedFilesManifest(skipped));
                zipOut.closeEntry();
            }
        }

        List<Map<String, Object>> skippedJobsJson = skipped.stream()
                .map(s -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("jobId", s.jobId());
                    entry.put("file_detail", s.label());
                    entry.put("reason", s.reason());
                    return entry;
                })
                .toList();

        logger.info("Zip download built: fileName={}, skippedJobs={}", zipFileName, skippedJobsJson);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fileName", zipFileName);
        body.put("data", Base64.getEncoder().encodeToString(baos.toByteArray()));
        body.put("skippedJobs", skippedJobsJson);

        return ResponseEntity.ok(body);
    }

    private String sanitizeFolderName(String name) {
        if (name == null || name.isEmpty()) {
            return "unnamed_project";
        }
        // Remove or replace special characters that aren't allowed in file paths
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    // Zip folder name for a job's sibling group (jobs created from the same uploaded
    // source file, one per target language). Falls back to the job's own id if the
    // job has no sourceGroupId or fileName for some reason.
    private String groupFolderName(Job job) {
        Long groupId = job.getSourceGroupId() != null ? job.getSourceGroupId() : job.getId();
        String fileName = job.getFileName() != null ? sanitizeFolderName(job.getFileName()) : "job" + job.getId();
        return groupId + "-" + fileName;
    }

    // Inserts the job's target language(s) before the file extension so sibling jobs
    // sharing the same source file (and therefore the same group folder/base filename)
    // don't collide in the zip, e.g. "Document.xliff" -> "Document_ko-KR.xliff".
    private String appendTargetLang(String fileName, Job job) {
        if (fileName == null) {
            return "job" + job.getId();
        }

        String targetLang = (job.getTargetLangs() != null && !job.getTargetLangs().isEmpty())
                ? job.getTargetLangs().stream().sorted().collect(Collectors.joining("-"))
                : "";

        if (targetLang.isEmpty()) {
            return fileName;
        }

        int dotIndex = fileName.lastIndexOf('.');
        String baseName = dotIndex != -1 ? fileName.substring(0, dotIndex) : fileName;
        String extension = dotIndex != -1 ? fileName.substring(dotIndex) : "";

        return baseName + "_" + targetLang + extension;
    }

    // Uploads or updates the translated file
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

    @PostMapping("/{jobId}/checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JobCheckoutStatusDTO> checkoutJob(
            @PathVariable Long jobId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String uid = userDetails.getUid();
        JobCheckoutStatusDTO dto = jobService.checkoutJob(jobId, uid);
        return ResponseEntity.ok(dto);
    }

    // Partial save: overwrite the working copy and refresh the lock's expiry, file stays checked out
    @PostMapping("/{jobId}/save-draft")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JobCheckoutStatusDTO> saveDraft(
            @PathVariable Long jobId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String uid = userDetails.getUid();
        JobCheckoutStatusDTO dto = jobService.saveDraft(jobId, uid, file);
        return ResponseEntity.ok(dto);
    }

    // Download the working copy so the editor can load what is currently checked out
    @GetMapping("/{jobId}/download/working-copy")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadWorkingCopy(@PathVariable Long jobId) {
        try {
            Path filePath = jobService.getWorkingCopyPath(jobId);

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                logger.error("Working copy not readable: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/xml"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filePath.getFileName() + "\"")
                .body(resource);

        } catch (ResourceNotFoundException e) {
            logger.error("File not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error downloading working copy for job: " + jobId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{jobId}/checkin")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JobDTO> checkinJob(
            @PathVariable Long jobId,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String uid = userDetails.getUid();
        JobDTO dto = jobService.checkinJob(jobId, uid, file);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{jobId}/cancel-checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JobDTO> cancelCheckoutJob(
            @PathVariable Long jobId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String uid = userDetails.getUid();
        JobDTO dto = jobService.cancelCheckoutJob(jobId, uid);
        return ResponseEntity.ok(dto);
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

    
    // Soft delete a single job
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDeleteJob(
            @PathVariable Long id,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String uid = userDetails.getUid();
        
        jobService.softDeleteJob(id, uid);
        return ResponseEntity.noContent().build();
    }
}
