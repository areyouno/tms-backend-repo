package com.tms.backend.jobAnalysis;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.JobAnalysisCreateDTO;
import com.tms.backend.dto.JobAnalysisResponseDTO;
import com.tms.backend.user.CustomUserDetails;
import com.tms.backend.user.User;
import com.tms.backend.user.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/jobAnalysis")
@RequiredArgsConstructor
public class JobAnalysisController {
    private final JobAnalysisService jobAnalysisService;
    private final UserService userService;

    /**
     * Creates a new job analysis with macro-resolved name.
     * The analysis name template from user settings will have macros replaced,
     * e.g., "Analysis {projectName}" becomes "Analysis MyProject"
     *
     * @param request The job analysis creation request containing jobId and languages
     * @param userDetails The authenticated user details
     * @return The created job analysis with resolved name
     */
    @PostMapping("/create")
    public ResponseEntity<JobAnalysisResponseDTO> createJobAnalysis(
            @RequestBody JobAnalysisCreateDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Get current user
        String uid = userDetails.getUid();
        User currentUser = userService.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("User not found with uid: " + uid));

        // Create job analysis with macro resolution
        JobAnalysisResponseDTO response = jobAnalysisService.createJobAnalysisFromJobIds(
            request.jobIds(),
            request.workflowStepId(),
            currentUser
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping()
    public ResponseEntity<List<JobAnalysisResponseDTO>> getAllJobAnalyses() {
        return ResponseEntity.ok(jobAnalysisService.getAllJobAnalyses());
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<JobAnalysisResponseDTO>> getJobAnalysesByProjectId(@PathVariable Long projectId) {
        return ResponseEntity.ok(jobAnalysisService.getJobAnalysesByProjectId(projectId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobAnalysisResponseDTO> getJobAnalysis(@PathVariable Long id) {
        return ResponseEntity.ok(jobAnalysisService.getJobAnalysis(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJobAnalysis(@PathVariable Long id) {
        jobAnalysisService.deleteJobAnalysis(id);
        return ResponseEntity.noContent().build();
    }
}
