package com.tms.backend.dto;

import java.time.LocalDateTime;

import com.tms.backend.project.Project;

public record ProjectSoftDeleteDTO(
    Long id,
    String name,
    String createdBy,
    LocalDateTime deletedDate,
    String deletedBy
) {
    public static ProjectSoftDeleteDTO from(Project project) {
        return new ProjectSoftDeleteDTO(
            project.getId(),
            project.getName(),
            project.getCreatedBy(),
            project.getDeletedDate(),
            project.getDeletedBy()
        );
    }
}
