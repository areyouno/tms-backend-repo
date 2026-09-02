package com.tms.backend.taskList;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

import com.tms.backend.job.Job;
import com.tms.backend.language.Language;
import com.tms.backend.user.User;
import com.tms.backend.workflowSteps.WorkflowStep;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "task_lists")
public class TaskList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String taskName;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime startDate;
    private LocalDateTime dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_lang_id")
    private Language targetLang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_step_id")
    private WorkflowStep workflowStep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id", referencedColumnName = "user_id")
    private User assignee;

    @ManyToMany
    @JoinTable(
        name = "task_list_jobs",
        joinColumns = @JoinColumn(name = "task_list_id"),
        inverseJoinColumns = @JoinColumn(name = "job_id")
    )
    private Set<Job> jobs = new HashSet<>();

    private String createdBy;

    @Column(name = "create_date", updatable = false)
    @CreationTimestamp
    private LocalDateTime createDate;

    // Tracks the background flow that sizes the task's files against a fresh template TM
    // in Tomato and assigns it to the workflow step/assignee. One of: PENDING, SIZING,
    // ASSIGNING, COMPLETED, FAILED, SKIPPED_NO_ASSIGNEE.
    private String tmProvisioningStatus;

    private String provisioningRequestedByUsername;

    private String templateTmSizingJobId;

    private Long templateTmId;

    private String templateTmName;

    private Integer templateTmUnitCount;

    private Long assignedTmId;

    private Boolean tmAssignWasExisting;

    @Column(columnDefinition = "TEXT")
    private String tmProvisioningError;

    public Long getId() { return id; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public Language getTargetLang() { return targetLang; }
    public void setTargetLang(Language targetLang) { this.targetLang = targetLang; }

    public WorkflowStep getWorkflowStep() { return workflowStep; }
    public void setWorkflowStep(WorkflowStep workflowStep) { this.workflowStep = workflowStep; }

    public User getAssignee() { return assignee; }
    public void setAssignee(User assignee) { this.assignee = assignee; }

    public Set<Job> getJobs() { return jobs; }
    public void setJobs(Set<Job> jobs) { this.jobs = jobs; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreateDate() { return createDate; }
    public void setCreateDate(LocalDateTime createDate) { this.createDate = createDate; }

    public String getTmProvisioningStatus() { return tmProvisioningStatus; }
    public void setTmProvisioningStatus(String tmProvisioningStatus) { this.tmProvisioningStatus = tmProvisioningStatus; }

    public String getProvisioningRequestedByUsername() { return provisioningRequestedByUsername; }
    public void setProvisioningRequestedByUsername(String provisioningRequestedByUsername) { this.provisioningRequestedByUsername = provisioningRequestedByUsername; }

    public String getTemplateTmSizingJobId() { return templateTmSizingJobId; }
    public void setTemplateTmSizingJobId(String templateTmSizingJobId) { this.templateTmSizingJobId = templateTmSizingJobId; }

    public Long getTemplateTmId() { return templateTmId; }
    public void setTemplateTmId(Long templateTmId) { this.templateTmId = templateTmId; }

    public String getTemplateTmName() { return templateTmName; }
    public void setTemplateTmName(String templateTmName) { this.templateTmName = templateTmName; }

    public Integer getTemplateTmUnitCount() { return templateTmUnitCount; }
    public void setTemplateTmUnitCount(Integer templateTmUnitCount) { this.templateTmUnitCount = templateTmUnitCount; }

    public Long getAssignedTmId() { return assignedTmId; }
    public void setAssignedTmId(Long assignedTmId) { this.assignedTmId = assignedTmId; }

    public Boolean getTmAssignWasExisting() { return tmAssignWasExisting; }
    public void setTmAssignWasExisting(Boolean tmAssignWasExisting) { this.tmAssignWasExisting = tmAssignWasExisting; }

    public String getTmProvisioningError() { return tmProvisioningError; }
    public void setTmProvisioningError(String tmProvisioningError) { this.tmProvisioningError = tmProvisioningError; }
}
