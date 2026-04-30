package com.tms.backend.jobAnalysis;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.tms.backend.dto.JobAnalysisCreateDTO;
import com.tms.backend.dto.JobAnalysisResponseDTO;
import com.tms.backend.dto.SizingStatusDTO;
import com.tms.backend.tomato.SizingPollService;
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
    private final SizingPollService sizingPollService;

    /**
     * Submits files to Tomato for sizing and returns a Tomato jobId immediately.
     * Poll GET /sizing-status/{tomatoJobId} to check when the result is ready.
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createJobAnalysis(
            @RequestBody JobAnalysisCreateDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String uid = userDetails.getUid();
        User currentUser = userService.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("User not found with uid: " + uid));

        String tomatoJobId = jobAnalysisService.initiateSizing(
                request.jobIds(),
                request.workflowStepId(),
                currentUser
        );

        return ResponseEntity.accepted().body(Map.of("tomatoJobId", tomatoJobId));
    }

    /**
     * Checks the sizing result for the given Tomato jobId.
     * Returns { "status": "pending" } while processing,
     * or { "status": "done", "result": {...} } when the JobAnalysis has been saved.
     */
    @GetMapping("/sizing-status/{tomatoJobId}")
    public ResponseEntity<SizingStatusDTO> getSizingStatus(@PathVariable String tomatoJobId) {
        SizingStatusDTO status = jobAnalysisService.getSizingStatus(tomatoJobId);
        return ResponseEntity.ok(status);
    }

    /**
     * Opens an SSE stream for the given Tomato jobId.
     * The frontend receives a "sizing-status" event with { "status": "completed" } or { "status": "failed" }
     * when the background polling finishes, then can fetch results from GET /project/{projectId}.
     */
    @GetMapping(value = "/sizing-sse/{tomatoJobId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sizingSse(@PathVariable String tomatoJobId) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        sizingPollService.registerEmitter(tomatoJobId, emitter);
        return emitter;
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
