package com.tms.backend.projectTemplate;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tms.backend.dto.ProjectTemplateCreateDTO;
import com.tms.backend.dto.ProjectTemplateDTO;
import com.tms.backend.exception.ResourceNotFoundException;

@Service
public class ProjectTemplateService {
    private final ProjectTemplateRepository templateRepository;

    public ProjectTemplateService(ProjectTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public List<ProjectTemplateDTO> getAllTemplates() {
        return templateRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    public ProjectTemplateDTO getTemplate(Long id) {
        return templateRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
    }

    public ProjectTemplateDTO createTemplate(ProjectTemplateCreateDTO dto) {
        ProjectTemplate template = new ProjectTemplate();
        applyCreateDTO(template, dto);
        return convertToDTO(templateRepository.save(template));
    }

    public ProjectTemplateDTO updateTemplate(Long id, ProjectTemplateCreateDTO dto) {
        ProjectTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));

        applyCreateDTO(template, dto);
        return convertToDTO(templateRepository.save(template));
    }

    public void deleteTemplate(Long id) {
        templateRepository.deleteById(id);
    }

    private void applyCreateDTO(ProjectTemplate template, ProjectTemplateCreateDTO dto) {
        template.setName(dto.name());
        template.setSourceLang(dto.sourceLang());
        template.setTargetLang(dto.targetLang());
        template.setMachineTranslationId(dto.machineTranslationId());
        template.setBusinessUnitId(dto.businessUnitId());
        template.setType(dto.type());
        template.setClientId(dto.clientId());
        template.setCostCenterId(dto.costCenterId());
        template.setDomainId(dto.domainId());
        template.setSubdomainId(dto.subdomainId());
        template.setWorkflowSteps(dto.workflowSteps());
    }

    private ProjectTemplateDTO convertToDTO(ProjectTemplate template) {
        return new ProjectTemplateDTO(
                template.getId(),
                template.getName(),
                template.getSourceLang(),
                template.getTargetLang(),
                template.getMachineTranslationId(),
                template.getBusinessUnitId(),
                template.getType(),
                template.getClientId(),
                template.getCostCenterId(),
                template.getDomainId(),
                template.getSubdomainId(),
                template.getWorkflowSteps()
        );
    }
}
