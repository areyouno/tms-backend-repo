package com.tms.backend.job;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tms.backend.dto.JobDTO;
import com.tms.backend.dto.JobWorkflowStepDTO;
import com.tms.backend.dto.JobWorkflowStepEditDTO;
import com.tms.backend.dto.ProjectWithJobDTO;
import com.tms.backend.user.CustomUserDetails;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

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
        jobService.deleteJob(id);
        return ResponseEntity.ok("Job deleted successfully");
    }
}
