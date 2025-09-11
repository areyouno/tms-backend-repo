package com.tms.backend.job;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.tms.backend.user.User;
import com.tms.backend.workflowSteps.WorkflowStep;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_workflow_step")
public class JobWorkflowStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_step_id")
    private WorkflowStep workflowStep;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", referencedColumnName = "user_id")
    private User provider;
    private LocalDateTime dueDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notify_user_id")
    private User notifyUser;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private JobWorkflowStatus workflowStatus = JobWorkflowStatus.NEW;
    
    private Integer stepOrder;
    
    @Column(name = "create_date", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "update_date")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Long getId() { return id; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public WorkflowStep getWorkflowStep() { return workflowStep; }
    public void setWorkflowStep(WorkflowStep workflowStep) { this.workflowStep = workflowStep; }
    
    public User getProvider() { return provider; }
    public void setProvider(User provider) { this.provider = provider; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public User getNotifyUser() { return notifyUser; }
    public void setNotifyUser(User notifyUser) { this.notifyUser = notifyUser; }

    public JobWorkflowStatus getWorkflowStatus() { return workflowStatus; }
    public void setWorkflowStatus(JobWorkflowStatus workflowStatus) { this.workflowStatus = workflowStatus; }

    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}