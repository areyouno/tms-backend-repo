package com.tms.backend.projectTmAssignment;

import com.tms.backend.project.Project;
import com.tms.backend.workflowSteps.WorkflowStep;

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
    uniqueConstraints = @UniqueConstraint(
        columnNames = {
            "project_id",
            "translation_memory_id",
            "workflow_step_id"
        }
    )
)
public class ProjectTmAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Project
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // Translation Memory
    @Column(name = "translation_memory_id", nullable = false)
    private Long tmId;

    // Workflow step
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_step_id", nullable = false)
    private WorkflowStep workflowStep;

    private boolean writeAccess;
    private Long penalty;
    private Long priorityOrder;

    // Assignment attributes
    private boolean readAccess;
    public boolean isReadAccess() {
        return readAccess;
    }

    public void setReadAccess(boolean readAccess) {
        this.readAccess = readAccess;
    }

    public boolean isWriteAccess() {
        return writeAccess;
    }

    public Long getTmId() {
        return tmId;
    }

    public void setTmId(Long tmId) {
        this.tmId = tmId;
    }

    public WorkflowStep getWorkflowStep() {
        return workflowStep;
    }

    public void setWorkflowStep(WorkflowStep workflowStep) {
        this.workflowStep = workflowStep;
    }

    public void setWriteAccess(boolean writeAccess) {
        this.writeAccess = writeAccess;
    }

    public Long getPenalty() {
        return penalty;
    }

    public void setPenalty(Long penalty) {
        this.penalty = penalty;
    }

    public Long getPriorityOrder() {
        return priorityOrder;
    }

    public void setPriorityOrder(Long priorityOrder) {
        this.priorityOrder = priorityOrder;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
