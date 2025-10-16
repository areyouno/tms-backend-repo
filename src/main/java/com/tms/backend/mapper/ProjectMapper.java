package com.tms.backend.mapper;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tms.backend.dto.ProjectDTO;
import com.tms.backend.project.Project;
import com.tms.backend.workflowSteps.WorkflowStep;

@Component
public class ProjectMapper {
    public ProjectDTO toFullDTO(Project project) {
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
            project.getDeletedDate());
    }
}
