package com.tms.backend.project;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.ProjectDTO;
import com.tms.backend.dto.ProjectSummaryDTO;
import com.tms.backend.user.CustomUserDetails;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectService projectService;
    
    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectSummaryDTO> getAllProjects(Authentication authentication){
        // Extract user details from JWT
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String uid = userDetails.getUid();

        return projectService.getProjectsByOwner(uid);
    }

    @GetMapping("/{id}")
    public ProjectDTO getProject(@PathVariable Long id, Authentication authentication) throws AccessDeniedException {
        ProjectDTO project = projectService.getProjectById(id);

        return project;
    }

    @PostMapping("/create")
    public ProjectDTO saveProject(@RequestBody ProjectDTO dto) {
        return projectService.save(
            dto.name(),
            dto.clientId(),
            dto.sourceLang(),
            dto.targetLang(),
            dto.businessUnitId(),
            dto.dueDate(),
            dto.purchaseOrder(),
            dto.costCenterId(),
            dto.domainId(),
            dto.subdomainId(),
            dto.workflowSteps(),
            dto.fileHandover()
        );
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProjectDTO> update(
        @PathVariable Long id,
        @RequestBody ProjectDTO dto)
        {
        ProjectDTO updated = projectService.update(id, dto);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok("Project deleted successfully");
    }

    // GET /projects/{projectId}/target-languages
    @GetMapping("/{projectId}/targetLanguages")
    public Set<String> getProjectTargetLanguages(@PathVariable Long projectId) {
        return projectService.getTargetLanguages(projectId);
    }

}
