package com.tms.backend.projectTemplate;

import java.util.List;

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



@RestController
@RequestMapping("/templates")
public class ProjectTemplateController {
    private final ProjectTemplateService templateService;

    public ProjectTemplateController(ProjectTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public List<ProjectTemplateDTO> getAllTemplates() {
        return templateService.getAllTemplates();
    }

    @GetMapping("/{id}")
    public ProjectTemplateDTO getTemplate(@PathVariable Long id) {
        return templateService.getTemplate(id);
    }

    @PostMapping
    public ProjectTemplateDTO createTemplate(@RequestBody ProjectTemplateCreateDTO dto) {
        return templateService.createTemplate(dto);
    }

    @PutMapping("/{id}")
    public ProjectTemplateDTO updateTemplate(@PathVariable Long id, @RequestBody ProjectTemplateCreateDTO dto) {
        return templateService.updateTemplate(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
    }

}
