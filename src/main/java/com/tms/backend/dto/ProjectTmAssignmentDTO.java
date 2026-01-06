package com.tms.backend.dto;

import com.tms.backend.projectTmAssignment.ProjectTmAssignment;

public record ProjectTmAssignmentDTO(
    Long tmId,
    boolean read,
    boolean write,
    Long penalty,
    Long priorityOrder,
    Long workflowStepId
) {
    public static ProjectTmAssignmentDTO fromEntity(ProjectTmAssignment a) {
        return new ProjectTmAssignmentDTO(
            a.getTmId(),
            a.isReadAccess(),
            a.isWriteAccess(),
            a.getPenalty(),
            a.getPriorityOrder(),
            a.getWorkflowStep().getId()
        );
    }
}
