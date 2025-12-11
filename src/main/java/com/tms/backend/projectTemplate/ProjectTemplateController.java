package com.tms.backend.projectTemplate;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.ProjectTemplateCreateDTO;
import com.tms.backend.dto.ProjectTemplateDTO;
import com.tms.backend.user.CustomUserDetails;



@RestController
@RequestMapping("/api/project-templates")
public class ProjectTemplateController {
    private final ProjectTemplateService templateService;

    public ProjectTemplateController(ProjectTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProjectTemplateDTO>> getAllTemplates(
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        boolean isAdmin = isAdmin(authentication);
        
        List<ProjectTemplateDTO> responses = templateService.getAllTemplates(currentUserId, isAdmin);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ProjectTemplateDTO getTemplate(@PathVariable Long id,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        boolean isAdmin = isAdmin(authentication);
        ProjectTemplateDTO response = templateService.getTemplateById(id, currentUserId, isAdmin);
        return response;
    }

    @PostMapping
    public ProjectTemplateDTO createTemplate(@RequestBody ProjectTemplateCreateDTO dto,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        ProjectTemplateDTO response = templateService.createTemplate(dto, currentUserId);
        return response;
    }

    @PutMapping("/{id}")
    public ProjectTemplateDTO updateTemplate(@PathVariable Long id, @RequestBody ProjectTemplateCreateDTO dto,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        boolean isAdmin = isAdmin(authentication);
        ProjectTemplateDTO response = templateService.updateTemplate(id, dto, currentUserId, isAdmin);
        return response;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTemplate(@PathVariable Long id,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        boolean isAdmin = isAdmin(authentication);
        String message = templateService.deleteTemplate(id, currentUserId, isAdmin);
        return ResponseEntity.ok(message);
    }

    /**
     * Extract the current user's ID from the authentication token
     * Adjust this method based on your authentication setup
     */
    private Long getCurrentUserId(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getId();
    }

    /**
     * Check if the current user has admin privileges
     * Checks for the administrator authority
     */
    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("administrator"));
    }

}
