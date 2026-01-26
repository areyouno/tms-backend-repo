package com.tms.backend.dto;

import com.tms.backend.projectTbAssignment.ProjectTbAssignment;

public record ProjectTbAssignmentDTO(
    Long tbId,
    boolean read,
    boolean write,
    boolean isQA
) {
    public static ProjectTbAssignmentDTO fromEntity(ProjectTbAssignment a) {
        return new ProjectTbAssignmentDTO(
            a.getTbId(), 
            a.isReadAccess(),
            a.isWriteAccess(),
            a.isQA()
        );
    }
}
