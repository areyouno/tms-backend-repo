package com.tms.backend.project;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tms.backend.businessUnit.BusinessUnit;
import com.tms.backend.businessUnit.BusinessUnitRepository;
import com.tms.backend.client.Client;
import com.tms.backend.client.ClientRepository;
import com.tms.backend.costCenter.CostCenter;
import com.tms.backend.costCenter.CostCenterRepository;
import com.tms.backend.domain.Domain;
import com.tms.backend.domain.DomainRepository;
import com.tms.backend.dto.ProjectCreateDTO;
import com.tms.backend.dto.ProjectDTO;
import com.tms.backend.dto.ProjectSummaryDTO;
import com.tms.backend.exception.ResourceNotFoundException;
import com.tms.backend.machineTranslation.MachineTranslation;
import com.tms.backend.machineTranslation.MachineTranslationRepository;
import com.tms.backend.subDomain.SubDomain;
import com.tms.backend.subDomain.SubDomainRepository;
import com.tms.backend.user.User;
import com.tms.backend.user.UserRepository;
import com.tms.backend.workflowSteps.WorkflowStep;
import com.tms.backend.workflowSteps.WorkflowStepRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ProjectService {

    private ProjectRepository projectRepo;
    private BusinessUnitRepository businessUnitRepo;
    private ClientRepository clientRepo;
    private DomainRepository domainRepo;
    private SubDomainRepository subDomainRepo;
    private UserRepository userRepo;
    private MachineTranslationRepository mtRepo;
    private BusinessUnitRepository buRepo;
    private CostCenterRepository ccRepo;
    private WorkflowStepRepository wfRepo;

    public ProjectService(
        ProjectRepository projectRepo,
        BusinessUnitRepository businessUnitRepo,
        ClientRepository clientRepo,
        DomainRepository domainRepo,
        SubDomainRepository subDomainRepo,
        UserRepository userRepo,
        MachineTranslationRepository mtRepo,
        BusinessUnitRepository buRepo,
        CostCenterRepository ccRepo,
        WorkflowStepRepository wfRepo
    ) {
        this.projectRepo = projectRepo;
        this.businessUnitRepo = businessUnitRepo;
        this.clientRepo = clientRepo;
        this.domainRepo = domainRepo;
        this.subDomainRepo = subDomainRepo;
        this.userRepo = userRepo;
        this.mtRepo = mtRepo;
        this.buRepo = buRepo;
        this.ccRepo = ccRepo;
        this.wfRepo = wfRepo;
    }

    public ProjectDTO createProject(ProjectCreateDTO createDTO, String userEmail) throws UsernameNotFoundException {
        // Create new project entity
        Project project = new Project();
        
        // Set fields from DTO
        project.setName(createDTO.name());
        project.setDueDate(createDTO.dueDate());
        project.setSourceLang(createDTO.sourceLang());
        project.setTargetLanguages(createDTO.targetLang());

        if (createDTO.machineTranslationId() != null) {
            MachineTranslation mt = mtRepo.findById(createDTO.machineTranslationId())
                .orElseThrow(() -> new RuntimeException("MachineTranslation not found: " + createDTO.machineTranslationId()));
            project.setMachineTranslation(mt);
        }

        if (createDTO.businessUnitId() != null) {
            BusinessUnit bu = buRepo.findById(createDTO.businessUnitId())
                .orElseThrow(() -> new RuntimeException("Business unit not found: " + createDTO.businessUnitId()));
            project.setBusinessUnit(bu);
        }

        project.setPurchaseOrderNum(createDTO.purchaseOrder());
        project.setType(createDTO.type());
        
        if (createDTO.clientId() != null) {
            Client cl = clientRepo.findById(createDTO.clientId())
                .orElseThrow(() -> new RuntimeException("Client not found: " + createDTO.businessUnitId()));
            project.setClient(cl);
        }

        project.setNote(createDTO.note());

        if (createDTO.costCenterId() != null) {
            CostCenter cc = ccRepo.findById(createDTO.costCenterId())
                    .orElseThrow(() -> new RuntimeException("Cost center not found: " + createDTO.costCenterId()));
            project.setCostCenter(cc);
        }

        if (createDTO.domainId() != null) {
            Domain domain = domainRepo.findById(createDTO.domainId())
                    .orElseThrow(() -> new RuntimeException("Cost center not found: " + createDTO.domainId()));
            project.setDomain(domain);
        }

        if (createDTO.subdomainId() != null) {
            SubDomain subdomain = subDomainRepo.findById(createDTO.subdomainId())
                    .orElseThrow(() -> new RuntimeException("Cost center not found: " + createDTO.subdomainId()));
            project.setSubdomain(subdomain);
        }

        if (createDTO.workflowSteps() != null && !createDTO.workflowSteps().isEmpty()) {
            Set<WorkflowStep> steps = createDTO.workflowSteps().stream()
                     .map(id -> wfRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("WorkflowStep not found: " + id)))
                    .collect(Collectors.toSet());
            project.setWorkflowSteps(steps);
        }
        
        User currentUser = userRepo.findByEmail(userEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));

        project.setOwner(currentUser);

        project.setCreatedBy(currentUser.getFirstName() + " " + currentUser.getLastName());
        project.setCreateDate(LocalDateTime.now());
        project.setStatus("Standard"); 

        // Save project
        Project savedProject = projectRepo.save(project);
        
        // Convert to response DTO
        return convertToFullDTO(savedProject);
    }

    @Transactional(readOnly = true)
    public List<ProjectSummaryDTO> getAllProjects() {
        return projectRepo.findAllActive()
                .stream()
                .map(this::convertToSummaryDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> getProjectsByOwner(String uid) {
        User user = userRepo.findByUid(uid)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Project> projects = projectRepo.findByOwnerId(user.getId());
        return projects.stream()
                .map(this::convertToFullDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectDTO getProjectById(Long id) throws AccessDeniedException {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepo.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Project project = projectRepo.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));

        if(!project.getOwner().getId().equals(user.getId())){
            throw new AccessDeniedException("Not allowed to view this project");
        }

        return convertToFullDTO(project);
    }

    // Get soft deleted projects for user
    public List<ProjectDTO> getSoftDeletedProjects(String uid) {
        List<Project> deletedProjects = projectRepo.findSoftDeletedByOwner(uid);
        return deletedProjects.stream()
            .map(this::convertToFullDTO)
            .collect(Collectors.toList());
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
        if (project == null) {
            return null;
        }

        return new ProjectDTO(
                project.getId(),
                project.getName(),
                project.getDueDate(),
                project.getSourceLang(),
                project.getTargetLanguages(),
                project.getMachineTranslation() != null ? project.getMachineTranslation().getId() : null,
                project.getBusinessUnit() != null ? project.getBusinessUnit().getId() : null,
                project.getPurchaseOrderNum(),
                project.getType(),
                project.getClient() != null ? project.getClient().getId() : null,
                project.getNote(),
                project.getCostCenter() != null ? project.getCostCenter().getId() : null,
                project.getDomain() != null ? project.getDomain().getId() : null,
                project.getSubdomain() != null ? project.getSubdomain().getId() : null,
                project.getWorkflowSteps().stream()
                .map(WorkflowStep::getId) // get uid of each workflow; .map(workflowStep ->
                .collect(Collectors.toSet()), // put them into Set<String>
                project.getOwner() != null ? project.getOwner().getUid() : null,
                project.getCreatedBy(),
                project.getCreateDate(),
                project.getStatus(),
                "0",
                project.getFileHandover(),
                project.isDeleted(),
                project.getDeletedBy(),
                project.getDeletedDate()
                );
    }

    @Transactional
    public ProjectDTO updateProject(Long id, ProjectDTO updatedData, String uid) {
        Project project = projectRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        if (!project.getOwner().getUid().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You cannot update this project");
        }

        if (updatedData.name() != null) {
            project.setName(updatedData.name());
        }

        if (updatedData.status() != null) {
            project.setStatus(updatedData.status());
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
    public void deleteProject(Long id, String uid) {
        Project project = projectRepo.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    
        if (!project.getOwner().getUid().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You cannot delete this project");
        }

        // soft delete
        project.setDeleted(true);
        project.setDeletedDate(LocalDateTime.now());

        // set deletedBy
        User currentUser = userRepo.findByUid(uid)
        .orElseThrow(() -> new RuntimeException("User not found"));
        project.setDeletedBy(currentUser.getFirstName() + " " + currentUser.getLastName());

        projectRepo.save(project);
    }

    public void hardDeleteProject(Long id, String uid) {
        // First check if project exists and user has permission
        Project project = projectRepo.findByIdIncludingDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
            
        if (!project.getOwner().getUid().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You cannot delete this project");
        }
        
        // Permanently delete from database
        projectRepo.hardDeleteById(id);
    }

    @Transactional
    public ProjectDTO restoreProject(Long id, String uid) {
        Project project = projectRepo.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.getOwner().getUid().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You cannot restore this project");
        }

        if (!project.isDeleted()) {
            throw new RuntimeException("Project is not deleted");
        }

        project.setDeleted(false);
        project.setDeletedDate(null);
        project.setDeletedBy(null);

        Project restored = projectRepo.save(project);
        return convertToFullDTO(restored);
    }

    @Transactional
    public Set<String> getTargetLanguages(Long projectId) {
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        return project.getTargetLanguages();
    }
}
