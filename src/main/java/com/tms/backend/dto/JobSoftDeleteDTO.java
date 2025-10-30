package com.tms.backend.dto;

import java.time.LocalDateTime;

import com.tms.backend.job.Job;

public record JobSoftDeleteDTO(
    Long id,
    String fileName,
    String createdBy,
    LocalDateTime deletedDate,
    String deletedBy
) {
    public static JobSoftDeleteDTO from(Job job) {
        String createdBy = null;
        if (job.getJobOwner() != null) {
            createdBy = job.getJobOwner().getFirstName() + " " + job.getJobOwner().getLastName();
        }
        
        return new JobSoftDeleteDTO(
            job.getId(),
            job.getFileName(),
            createdBy,
            job.getDeletedDate(),
            job.getDeletedBy()
        );
    }
}
