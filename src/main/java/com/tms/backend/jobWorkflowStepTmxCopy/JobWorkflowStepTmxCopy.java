package com.tms.backend.jobWorkflowStepTmxCopy;

import java.time.LocalDateTime;

import com.tms.backend.job.JobWorkflowStep;
import com.tms.backend.projectTmAssignment.ProjectTmAssignment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "job_workflow_step_tmx_copy",
    uniqueConstraints = @UniqueConstraint(
        columnNames = { "job_workflow_step_id", "project_tm_assignment_id" }
    )
)
public class JobWorkflowStepTmxCopy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_workflow_step_id", nullable = false)
    private JobWorkflowStep jobWorkflowStep;

    // Source assignment this copy was derived from (tmId, language pair, read/write, penalty, etc.)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_tm_assignment_id", nullable = false)
    private ProjectTmAssignment projectTmAssignment;

    @Column(length = 1024)
    private String tmxFilePath;
    private Long tmxFileSizeBytes;
    private LocalDateTime tmxCopiedAt;

    public Long getId() { return id; }

    public JobWorkflowStep getJobWorkflowStep() { return jobWorkflowStep; }
    public void setJobWorkflowStep(JobWorkflowStep jobWorkflowStep) { this.jobWorkflowStep = jobWorkflowStep; }

    public ProjectTmAssignment getProjectTmAssignment() { return projectTmAssignment; }
    public void setProjectTmAssignment(ProjectTmAssignment projectTmAssignment) { this.projectTmAssignment = projectTmAssignment; }

    public String getTmxFilePath() { return tmxFilePath; }
    public void setTmxFilePath(String tmxFilePath) { this.tmxFilePath = tmxFilePath; }

    public Long getTmxFileSizeBytes() { return tmxFileSizeBytes; }
    public void setTmxFileSizeBytes(Long tmxFileSizeBytes) { this.tmxFileSizeBytes = tmxFileSizeBytes; }

    public LocalDateTime getTmxCopiedAt() { return tmxCopiedAt; }
    public void setTmxCopiedAt(LocalDateTime tmxCopiedAt) { this.tmxCopiedAt = tmxCopiedAt; }
}
