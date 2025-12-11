package com.tms.backend.projectTemplate;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tms.backend.businessUnit.BusinessUnit;
import com.tms.backend.businessUnit.BusinessUnitRepository;
import com.tms.backend.client.Client;
import com.tms.backend.client.ClientRepository;
import com.tms.backend.costCenter.CostCenter;
import com.tms.backend.costCenter.CostCenterRepository;
import com.tms.backend.domain.Domain;
import com.tms.backend.domain.DomainRepository;
import com.tms.backend.dto.ProjectTemplateCreateDTO;
import com.tms.backend.dto.ProjectTemplateDTO;
import com.tms.backend.dto.ReferenceDTO;
import com.tms.backend.exception.ResourceNotFoundException;
import com.tms.backend.subDomain.SubDomain;
import com.tms.backend.subDomain.SubDomainRepository;
import com.tms.backend.user.User;
import com.tms.backend.user.UserRepository;
import com.tms.backend.vendor.Vendor;
import com.tms.backend.vendor.VendorRepository;

@Service
public class ProjectTemplateService {
    private final ProjectTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final BusinessUnitRepository businessUnitRepository;
    private final ClientRepository clientRepository;
    private final CostCenterRepository costCenterRepository;
    private final DomainRepository domainRepository;
    private final SubDomainRepository subDomainRepository;
    private final VendorRepository vendorRepository;

    public ProjectTemplateService(ProjectTemplateRepository templateRepository,
            UserRepository userRepository,
            BusinessUnitRepository businessUnitRepository, 
            ClientRepository clientRepository,
            CostCenterRepository costCenterRepository,
            DomainRepository domainRepository,
            SubDomainRepository subDomainRepository,
            VendorRepository vendorRepository) {
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
        this.businessUnitRepository = businessUnitRepository;
        this.clientRepository = clientRepository;
        this.costCenterRepository = costCenterRepository;
        this.domainRepository = domainRepository;
        this.subDomainRepository = subDomainRepository;
        this.vendorRepository = vendorRepository;
    }

    @Transactional
    public ProjectTemplateDTO createTemplate(ProjectTemplateCreateDTO dto, Long userId) {
        ProjectTemplate template = new ProjectTemplate();

        User creator = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        template.setCreatedBy(creator);

        template.setCreatedBy(creator);
        applyCreateDTO(template, dto);
        ProjectTemplate saved = templateRepository.save(template);
        return convertToDTO(saved);
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

        if (dto.ownerId() != null) {
            User owner = userRepository.findById(dto.ownerId())
                    .orElseThrow(() -> new RuntimeException("Owner not found"));
            template.setOwner(owner);
        }

        template.setSourceLang(dto.sourceLang());
        template.setTargetLang(dto.targetLang());
        template.setMachineTranslationId(dto.machineTranslationId());

        if (dto.businessUnitId() != null) {
            BusinessUnit bu = businessUnitRepository.findById(dto.businessUnitId())
                    .orElseThrow(() -> new RuntimeException("Business unit not found"));
            template.setBusinessUnit(bu);
        }

        template.setType(dto.type());
        
        if (dto.clientId() != null) {
            Client client = clientRepository.findById(dto.clientId())
                    .orElseThrow(() -> new RuntimeException("Client not found"));
            template.setClient(client);
        }

        if (dto.costCenterId() != null) {
            CostCenter cc = costCenterRepository.findById(dto.costCenterId())
                    .orElseThrow(() -> new RuntimeException("Cost center not found"));
            template.setCostCenter(cc);
        }

        if (dto.domainId() != null) {
            Domain domain = domainRepository.findById(dto.domainId())
                    .orElseThrow(() -> new RuntimeException("Domain not found"));
            template.setDomain(domain);
        }

        if (dto.subdomainId() != null) {
            SubDomain sub = subDomainRepository.findById(dto.subdomainId())
                    .orElseThrow(() -> new RuntimeException("Subdomain not found"));
            template.setSubdomain(sub);
        }

        if (dto.vendorId() != null) {
            Vendor vendor = vendorRepository.findById(dto.vendorId())
                    .orElseThrow(() -> new RuntimeException("Vendor not found"));
            template.setVendor(vendor);
        }
        
        template.setWorkflowSteps(dto.workflowSteps());

        TemplateStatusAutomationSetting setting = new TemplateStatusAutomationSetting();
        setting.setEnabledRules(dto.enabledRules() != null ? dto.enabledRules() : new HashSet<>());

        template.setStatusAutomationSetting(setting);

        template.setNote(dto.note());

        template.setCreatedDate(LocalDateTime.now());
    }

    private ProjectTemplateDTO convertToDTO(ProjectTemplate template) {
        return new ProjectTemplateDTO(
                template.getId(),
                template.getName(),
                template.getProjectName(),
                template.getUserId(),
                template.getOwner() != null ? new ReferenceDTO(template.getOwner().getId(), template.getOwner().getFirstName() + " " + template.getOwner().getLastName()) : null,
                template.getSourceLang(),
                template.getTargetLang(),
                template.getMachineTranslationId(),
                template.getBusinessUnit() != null ? new ReferenceDTO(template.getBusinessUnit().getId(), template.getBusinessUnit().getName()) : null,
                template.getType(),
                template.getClient() != null ? new ReferenceDTO(template.getClient().getId(), template.getClient().getName()) : null,
                template.getCostCenter() != null ? new ReferenceDTO(template.getCostCenter().getId(), template.getCostCenter().getName()) : null,
                template.getDomain() != null ? new ReferenceDTO(template.getDomain().getId(), template.getDomain().getName()) : null,
                template.getSubdomain() != null ? new ReferenceDTO(template.getSubdomain().getId(), template.getSubdomain().getName()) : null,
                template.getVendor() != null ? new ReferenceDTO(template.getVendor().getId(), template.getVendor().getName()) : null,
                template.getWorkflowSteps(),
                template.getStatusAutomationSetting() != null
                        ? template.getStatusAutomationSetting().getEnabledRules()
                        : null,
                template.getNote(),
                template.getCreatedBy() != null ? new ReferenceDTO(template.getCreatedBy().getId(), template.getCreatedBy().getFirstName() + " " + template.getCreatedBy().getLastName()) : null,
                template.getCreatedDate()
        );
    }
}
