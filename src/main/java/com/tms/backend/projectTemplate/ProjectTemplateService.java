package com.tms.backend.projectTemplate;

import java.util.HashSet;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tms.backend.dto.ProjectTemplateCreateDTO;
import com.tms.backend.dto.ProjectTemplateDTO;
import com.tms.backend.exception.ResourceNotFoundException;

@Service
public class ProjectTemplateService {
    private final ProjectTemplateRepository templateRepository;

    public ProjectTemplateService(ProjectTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Transactional
    public ProjectTemplateDTO createTemplate(ProjectTemplateCreateDTO dto, Long userId) {
        ProjectTemplate template = new ProjectTemplate();
        template.setUserId(userId); // Set the creator as current user
        applyCreateDTO(template, dto);
        return convertToDTO(templateRepository.save(template));
    }

    @Transactional(readOnly = true)
    public List<ProjectTemplateDTO> getAllTemplates(Long currentUserId, boolean isAdmin) {
        List<ProjectTemplate> templates;
        
        if (isAdmin) {
            // Admin can see all templates
            templates = templateRepository.findAll();
        } else {
            // Regular users can only see their own templates
            templates = templateRepository.findByUserId(currentUserId);
        }
        
        return templates.stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectTemplateDTO getTemplateById(Long id, Long currentUserId, boolean isAdmin) {
        ProjectTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
        
        // Check authorization: only owner or admin can view
        if (!isAdmin && !template.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("You don't have permission to access this template");
        }
        
        return convertToDTO(template);
    }

    @Transactional
    public ProjectTemplateDTO updateTemplate(Long id, ProjectTemplateCreateDTO dto, Long currentUserId, boolean isAdmin) {
        ProjectTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
        
        // Check authorization: only owner or admin can update
        if (!isAdmin && !template.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("You don't have permission to update this template");
        }
        
        applyCreateDTO(template, dto);
        // Don't allow changing the userId
        template.setUserId(template.getUserId());
        
        return convertToDTO(templateRepository.save(template));
    }

    @Transactional
    public String deleteTemplate(Long id, Long currentUserId, boolean isAdmin) {
        ProjectTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
        
        // Check authorization: only owner or admin can delete
        if (!isAdmin && !template.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("You don't have permission to delete this template");
        }
        
        templateRepository.deleteById(id);
        return "Template deleted successfully with id: " + id;
    }

    private void applyCreateDTO(ProjectTemplate template, ProjectTemplateCreateDTO dto) {
        template.setName(dto.name());
        template.setProjectName(dto.projectName());
        template.setUserId(dto.userId());
        template.setOwnerId(dto.ownerId());
        template.setSourceLang(dto.sourceLang());
        template.setTargetLang(dto.targetLang());
        template.setMachineTranslationId(dto.machineTranslationId());
        template.setBusinessUnitId(dto.businessUnitId());
        template.setType(dto.type());
        template.setClientId(dto.clientId());
        template.setCostCenterId(dto.costCenterId());
        template.setDomainId(dto.domainId());
        template.setSubdomainId(dto.subdomainId());
        template.setVendorId(dto.vendorId());
        template.setWorkflowSteps(dto.workflowSteps());

        TemplateStatusAutomationSetting setting = new TemplateStatusAutomationSetting();
        setting.setEnabledRules(dto.enabledRules() != null ? dto.enabledRules() : new HashSet<>());

        template.setStatusAutomationSetting(setting);

        template.setNote(dto.note());
    }

    private ProjectTemplateDTO convertToDTO(ProjectTemplate template) {
        return new ProjectTemplateDTO(
                template.getId(),
                template.getName(),
                template.getProjectName(),
                template.getUserId(),
                template.getOwnerId(),
                template.getSourceLang(),
                template.getTargetLang(),
                template.getMachineTranslationId(),
                template.getBusinessUnitId(),
                template.getType(),
                template.getClientId(),
                template.getCostCenterId(),
                template.getDomainId(),
                template.getSubdomainId(),
                template.getVendorId(),
                template.getWorkflowSteps(),
                template.getStatusAutomationSetting() != null
                    ? template.getStatusAutomationSetting().getEnabledRules()
                    : null,
                template.getNote()
        );
    }
}
