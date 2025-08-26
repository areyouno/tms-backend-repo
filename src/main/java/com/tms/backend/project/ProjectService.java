package com.tms.backend.project;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tms.backend.businessUnit.BusinessUnit;
import com.tms.backend.businessUnit.BusinessUnitRepository;
import com.tms.backend.client.Client;
import com.tms.backend.client.ClientRepository;
import com.tms.backend.domain.Domain;
import com.tms.backend.domain.DomainRepository;
import com.tms.backend.dto.ProjectDTO;
import com.tms.backend.dto.ProjectSummaryDTO;
import com.tms.backend.exception.ResourceNotFoundException;
import com.tms.backend.subDomain.SubDomain;
import com.tms.backend.subDomain.SubDomainRepository;
import com.tms.backend.user.User;
import com.tms.backend.user.UserRepository;
import com.tms.backend.workflowSteps.WorkflowSteps;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ProjectService {

    private ProjectRepository projectRepo;
    private BusinessUnitRepository businessUnitRepo;
    private ClientRepository clientRepo;
    private DomainRepository domainRepo;
    private SubDomainRepository subDomainRepo;
    private UserRepository userRepo;

    public ProjectService(
        ProjectRepository projectRepo,
        BusinessUnitRepository businessUnitRepo,
        ClientRepository clientRepo,
        DomainRepository domainRepo,
        SubDomainRepository subDomainRepo,
        UserRepository userRepo
    ) {
        this.projectRepo = projectRepo;
        this.businessUnitRepo = businessUnitRepo;
        this.clientRepo = clientRepo;
        this.domainRepo = domainRepo;
        this.subDomainRepo = subDomainRepo;
        this.userRepo = userRepo;
    }

    @Transactional(readOnly = true)
    public List<ProjectSummaryDTO> getAllProjects() {
        return projectRepo.findAll()
                .stream()
                .map(this::convertToSummaryDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectSummaryDTO> getProjectsByOwner(String uid) {
        List<Project> projects = projectRepo.findByOwnerUid(uid);
        return projects.stream()
                .map(this::convertToSummaryDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectDTO getProjectById(Long id) throws AccessDeniedException {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepo.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Project project = projectRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));

        if(!project.getOwner().getId().equals(user.getId())){
            throw new AccessDeniedException("Not allowed to view this project");
        }

        return convertToFullDTO(project);
    }

    private ProjectSummaryDTO convertToSummaryDTO(Project project) {
        String ownerName = null;
        if (project.getOwner() != null) {
            String lastName = project.getOwner().getLastName();
            String firstName = project.getOwner().getFirstName();
            ownerName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
            if (ownerName.isEmpty()) {
                ownerName = null;
            }
        }
        return new ProjectSummaryDTO(
                project.getId(),
                project.getName(),
                project.getCreateDate(),
                project.getClient() != null ? project.getClient().getName() : null,
                ownerName,
                project.getStatus(),
                project.getDueDate(),
                project.getTargetLanguages(),
                project.getProgress());
    }

    private ProjectDTO convertToFullDTO(Project project) {
        return new ProjectDTO(
                project.getId(),
                project.getName(),
                project.getCreatedBy(),
                project.getCreateDate(),
                project.getStatus(),
                project.getDueDate(),
                project.getSourceLang(),
                project.getTargetLanguages(),
                project.getMachineTranslation() != null ? project.getMachineTranslation().getId() : null,
                project.getOwner() != null ? project.getOwner().getUid() : null,
                project.getBusinessUnit() != null ? project.getBusinessUnit().getId() : null,
                project.getPurchaseOrderNum(),
                project.getType(),
                project.getClient() != null ? project.getClient().getId() : null,
                project.getNote(),
                project.getCostCenter() != null ? project.getCostCenter().getId() : null,
                project.getDomain() != null ? project.getDomain().getId() : null,
                project.getSubdomain() != null ? project.getSubdomain().getId() : null,
                project.getWorkflowSteps().stream()
                        .map(WorkflowSteps::getId) // get uid of each workflow; .map(workflowStep ->
                        .collect(Collectors.toSet()), // put them into Set<String>
                project.getFileHandover());
    }

     @Transactional
    public ProjectDTO save(String name, Long clientId, String sourceLang, Set<String> targetLangs,
            Long businessUnitId, LocalDateTime dueDate, String purchaseOrder, Long costCenterId, Long domainId,
            Long subdomainId, Set<Long> workFlowSteps, Boolean fileHandover) {

        Project project = new Project();
        project.setName(name);
        project.setSourceLang(sourceLang);
        project.setTargetLanguages(targetLangs);
        project.setDueDate(dueDate);
        project.setPurchaseOrderNum(purchaseOrder);

        if (businessUnitId != null) {
            BusinessUnit bu = businessUnitRepo.findById(businessUnitId)
                    .orElseThrow(() -> new EntityNotFoundException("Business Unit not found"));
            project.setBusinessUnit(bu);
        }

        if (clientId != null) {
            Client client = clientRepo.findById(clientId)
                    .orElseThrow(() -> new EntityNotFoundException("Client not found"));
            project.setClient(client);
        }

        if (domainId != null) {
            Domain domain = domainRepo.findById(domainId)
                    .orElseThrow(() -> new EntityNotFoundException("Domain not found"));
            project.setDomain(domain);
        }

        if (subdomainId != null) {
            SubDomain subDomain = subDomainRepo.findById(subdomainId)
                    .orElseThrow(() -> new EntityNotFoundException("Subdomain not found"));
            project.setSubdomain(subDomain);
        }

        Project saved = projectRepo.save(project);
        return convertToFullDTO(saved);
    }

    @Transactional
    public ProjectDTO update(Long id, ProjectDTO updatedData) {
        Project project = projectRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        if (updatedData.name() != null) {
            project.setName(updatedData.name());
        }

        if (updatedData.sourceLang() != null) {
            project.setSourceLang(updatedData.sourceLang());
        }

        if (updatedData.targetLang() != null) {
            project.setTargetLanguages(updatedData.targetLang());
        }

        if (updatedData.businessUnitId() != null) {
            BusinessUnit bu = businessUnitRepo.findById(updatedData.businessUnitId())
                    .orElseThrow(() -> new EntityNotFoundException("Business Unit not found"));
            project.setBusinessUnit(bu);
        }

        if (updatedData.dueDate() != null) {
            project.setDueDate(updatedData.dueDate());
        }

        if (updatedData.clientId() != null) {
            Client cl = clientRepo.findById(updatedData.clientId())
            .orElseThrow(() -> new EntityNotFoundException("Client not found"));
            project.setClient(cl);
        }

        if (updatedData.note() != null) {
            project.setNote(updatedData.note());
        }

        if (updatedData.purchaseOrder() != null) {
            project.setPurchaseOrderNum(updatedData.purchaseOrder());
        }

        if (updatedData.ownerUid() != null) {
        User newOwner = userRepo.findByUid(updatedData.ownerUid())
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
            project.setOwner(newOwner);
        }


        Project saved = projectRepo.save(project);
        return convertToFullDTO(saved);
    }

     @Transactional
    public void deleteProject(Long id) {
        if (!projectRepo.existsById(id)) {
            throw new ResourceNotFoundException("Project not found with id: " + id);
        }

        projectRepo.deleteById(id);
    }

    @Transactional
    public Set<String> getTargetLanguages(Long projectId) {
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        return project.getTargetLanguages();
    }
}
