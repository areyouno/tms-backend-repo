package com.tms.backend.dto;

import com.tms.backend.job.Job;
import com.tms.backend.job.JobWorkflowStatus;
import com.tms.backend.job.JobWorkflowStep;

public record TaskListItemDTO(
    Long jobId,
    String fileName,
    Job.OriginalFileFormat fileFormat,
    Long wordCount,
    Long progress,
    Long jobWorkflowStepId,
    JobWorkflowStatus status
) {
    public static TaskListItemDTO from(Job job, JobWorkflowStep jobWorkflowStep) {
        return new TaskListItemDTO(
            job.getId(),
            job.getFileName(),
            job.getOriginalFileFormat(),
            job.getWordCount(),
            job.getProgress(),
            jobWorkflowStep != null ? jobWorkflowStep.getId() : null,
            jobWorkflowStep != null ? jobWorkflowStep.getStatus() : null
        );
    }
}
