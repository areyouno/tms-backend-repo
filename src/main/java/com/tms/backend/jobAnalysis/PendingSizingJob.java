package com.tms.backend.jobAnalysis;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.tms.backend.user.User;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pending_sizing_jobs")
public class PendingSizingJob {

    @Id
    @Column(name = "tomato_job_id")
    private String tomatoJobId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pending_sizing_job_ids", joinColumns = @JoinColumn(name = "tomato_job_id"))
    @Column(name = "job_id")
    private List<Long> jobIds;

    @Column(name = "workflow_step_id")
    private Long workflowStepId;

    @Column(name = "project_id")
    private Long projectId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public PendingSizingJob() {}

    public PendingSizingJob(String tomatoJobId, List<Long> jobIds, Long workflowStepId, Long projectId, User user) {
        this.tomatoJobId = tomatoJobId;
        this.jobIds = jobIds;
        this.workflowStepId = workflowStepId;
        this.projectId = projectId;
        this.user = user;
    }

    public String getTomatoJobId() { return tomatoJobId; }
    public List<Long> getJobIds() { return jobIds; }
    public Long getWorkflowStepId() { return workflowStepId; }
    public Long getProjectId() { return projectId; }
    public User getUser() { return user; }
}
