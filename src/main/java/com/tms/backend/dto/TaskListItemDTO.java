package com.tms.backend.dto;

import com.tms.backend.job.Job;
import com.tms.backend.job.JobWorkflowStatus;

public record TaskListItemDTO(
    Long jobId,
    String fileName,
    Job.OriginalFileFormat fileFormat,
    Long wordCount,
    Long progress,
    JobWorkflowStatus status
) {
    public static TaskListItemDTO from(Job job, JobWorkflowStatus status) {
        return new TaskListItemDTO(
            job.getId(),
            job.getFileName(),
            job.getOriginalFileFormat(),
            job.getWordCount(),
            job.getProgress(),
            status
        );
    }
}
