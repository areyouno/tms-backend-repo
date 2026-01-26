package com.tms.backend.project;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.JobDTO;
import com.tms.backend.dto.ProjectCreateDTO;
import com.tms.backend.dto.ProjectDTO;
import com.tms.backend.dto.ProjectSoftDeleteDTO;
import com.tms.backend.dto.ProjectTbAssignmentDTO;
import com.tms.backend.dto.ProjectTbAssignmentRequest;
import com.tms.backend.dto.ProjectTmAssignmentDTO;
import com.tms.backend.dto.ProjectTmAssignmentRequest;
import com.tms.backend.job.JobService;
import com.tms.backend.projectTbAssignment.ProjectTbAssignmentService;
import com.tms.backend.projectTmAssignment.ProjectTmAssignmentService;
import com.tms.backend.user.CustomUserDetails;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final JobService jobService;
    private final ProjectTmAssignmentService tmAssignmentService;
    private final ProjectTbAssignmentService tbAssignmentService;
    
    public ProjectController(
        ProjectService projectService,
        JobService jobService,
        ProjectTmAssignmentService tmAssignmentService,
        ProjectTbAssignmentService tbAssignmentService) {
        this.projectService = projectService;
        this.jobService = jobService;
        this.tmAssignmentService = tmAssignmentService;
        this.tbAssignmentService = tbAssignmentService;
    }

    @PostMapping("/create")
    public ResponseEntity<ProjectDTO> createProject(
            @Valid @RequestBody ProjectCreateDTO createDTO,
            Authentication authentication) {

        String email = authentication.getName();

        ProjectDTO createdProject = projectService.createProject(createDTO, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProject);
    }

    @PostMapping("/{projectId}/assign-TMs")
    public ResponseEntity<List<ProjectTmAssignmentDTO>> assignTM(
        @PathVariable Long projectId,
        @RequestBody ProjectTmAssignmentRequest request) {
            List<ProjectTmAssignmentDTO> savedAssignments = tmAssignmentService.assignTMs(projectId, request);
            return ResponseEntity.ok(savedAssignments);
    }

    @PostMapping("{projectId}/assign-TBs")
    public ResponseEntity<List<ProjectTbAssignmentDTO>> assignTB(
        @PathVariable Long projectId,
        @RequestBody ProjectTbAssignmentRequest request
    ) {
        List<ProjectTbAssignmentDTO> savedAssignments = tbAssignmentService.assignTBs(projectId, request);
        return ResponseEntity.ok(savedAssignments);
    }

    @GetMapping
    public List<ProjectDTO> getAllProjects(Authentication authentication){
        // Extract user details from JWT
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String uid = userDetails.getUid();

        return projectService.getProjectsByOwner(uid);
    }

    // @GetMapping("/{id}")
    // public ProjectDTO getProject(@PathVariable Long id, Authentication authentication) throws AccessDeniedException {
    //     ProjectDTO project = projectService.getProjectById(id);

    //     return project;
    // }

    @GetMapping("/{id}")
    public ProjectDTO getProject(@PathVariable Long id, Authentication authentication) throws AccessDeniedException {
        return projectService.getProjectById(id);
    }

    @GetMapping("/{projectId}/jobs")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<JobDTO> getJobsByProject(@PathVariable Long projectId) {
        return jobService.getJobsByProjectId(projectId);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProjectDTO> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectDTO updateDTO,
            Authentication authentication) {
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String uid = userDetails.getUid();
        
        ProjectDTO updatedProject = projectService.updateProject(id, updateDTO, uid);
        return ResponseEntity.ok(updatedProject);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String uid = userDetails.getUid();
        
        projectService.deleteProject(id, uid);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteProject(@PathVariable Long id, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String uid = userDetails.getUid();
        projectService.hardDeleteProject(id, uid);
        return ResponseEntity.noContent().build();
    }

    // Restore soft deleted project
    @PatchMapping("/{id}/restore")
    public ResponseEntity<ProjectDTO> restoreProject(@PathVariable Long id, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String uid = userDetails.getUid();
        ProjectDTO restored = projectService.restoreProject(id, uid);
        return ResponseEntity.ok(restored);
    }

    @GetMapping("/deleted")
    public ResponseEntity<List<ProjectSoftDeleteDTO>> getSoftDeletedProjects(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String uid = userDetails.getUid();
        List<ProjectSoftDeleteDTO> deletedProjects = projectService.getSoftDeletedProjects(uid);
        return ResponseEntity.ok(deletedProjects);
    }

    @GetMapping("/{projectId}/targetLanguages")
    public Set<String> getProjectTargetLanguages(@PathVariable Long projectId) {
        return projectService.getTargetLanguages(projectId);
    }

}
