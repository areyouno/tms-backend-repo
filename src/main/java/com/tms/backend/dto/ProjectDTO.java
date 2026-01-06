package com.tms.backend.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import com.tms.backend.project.Project;
import com.tms.backend.workflowSteps.WorkflowStep;

public record ProjectDTO (
    Long id,
    String name,
    LocalDateTime dueDate,
    String sourceLang,
    Set<String> targetLang,
    Long machineTranslationId,
    Long businessUnitId,
    String purchaseOrder,
    String type,
    Long clientId,
    String note,
    Long costCenterId,
    Long domainId,
    Long subdomainId,
    Set<Long> workflowSteps,
    String ownerUid,
    String createdBy,
    LocalDateTime createDate,
    String status,
    String progress,
    Boolean fileHandover,
    boolean deleted,
    String deletedBy,
    LocalDateTime deletedDate,
    Set<String> automationRules,

    Set<ProjectTmAssignmentDTO> assignedTMs
) {

    public static ProjectDTO fromEntity(Project project) {

        Set<Long> workflowStepIds =
            project.getWorkflowSteps()
                   .stream()
                   .map(WorkflowStep::getId)
                   .collect(Collectors.toSet());

        Set<String> automationRules =
            project.getStatusAutomationSetting() != null
                ? project.getStatusAutomationSetting()
                         .getEnabledRules()
                         .stream()
                         .map(Enum::name)
                         .collect(Collectors.toSet())
                : Set.of();

        Set<ProjectTmAssignmentDTO> assignedTMs =
            project.getTmAssignments()
                   .stream()
                   .map(ProjectTmAssignmentDTO::fromEntity)
                   .collect(Collectors.toSet());

        return new ProjectDTO(
            project.getId(),
            project.getName(),
            project.getDueDate(),
            project.getSourceLang(),
            project.getTargetLanguages(),
            project.getMachineTranslation() != null
                ? project.getMachineTranslation().getId()
                : null,

            project.getBusinessUnit() != null
                ? project.getBusinessUnit().getId()
                : null,

            project.getPurchaseOrderNum(),
            project.getType(),
            project.getClient() != null
                ? project.getClient().getId()
                : null,

            project.getNote(),
            project.getCostCenter() != null
                ? project.getCostCenter().getId()
                : null,

            project.getDomain() != null
                ? project.getDomain().getId()
                : null,

            project.getSubdomain() != null
                ? project.getSubdomain().getId()
                : null,

            workflowStepIds,
            project.getOwner() != null
                ? project.getOwner().getUid()
                : null,
                
            project.getCreatedBy(),
            project.getCreateDate(),
            project.getStatus(),
            project.getProgress() != null
                ? project.getProgress().toPlainString()
                : null,

            project.getFileHandover(),
            project.isDeleted(),
            project.getDeletedBy(),
            project.getDeletedDate(),

            automationRules,
            assignedTMs
        );
    }
}
