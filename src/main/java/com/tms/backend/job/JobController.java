package com.tms.backend.job;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tms.backend.dto.JobAnalyticsCountDTO;
import com.tms.backend.dto.JobDTO;
import com.tms.backend.dto.JobEditDTO;
import com.tms.backend.dto.JobSearchFilterByDate;

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
    
    @Value("${file.upload-dir}")
    private String baseUploadDir;

    public Path getUserUploadPath(Long userId) {
        return Paths.get(baseUploadDir, String.valueOf(userId));
    }

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> uploadFileToLocal(
        @RequestPart("file") MultipartFile file,
        @RequestPart("job") JobDTO jobDTO) {
            try {
                JobDTO savedJob = jobService.saveFileToLocal(file, jobDTO);
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

    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<JobDTO> getAllJobs() {
        return jobService.getJobs();
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobEditDTO> update(@PathVariable Long id, @RequestBody JobEditDTO dto) {
        JobEditDTO updated = jobService.update(id, dto);
        
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteJob(@PathVariable Long id){
        jobService.deleteJob(id);
        return ResponseEntity.ok("Job deleted successfully");
    }

    @GetMapping("/analytics/filter")
    public JobAnalyticsCountDTO searchJobs(@RequestBody(required = false) JobSearchFilterByDate filter) {
        if (filter == null) {
            // no filter -> create empty object
            filter = new JobSearchFilterByDate(null, null, null, null, null);
        }
        return jobService.getJobCountByDate(filter);
    }

}
