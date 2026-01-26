package com.tms.backend.projectTbAssignment;

import com.tms.backend.project.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class ProjectTbAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Project
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "term_base_id", nullable = false)
    private Long tbId;

    private boolean readAccess;
    private boolean writeAccess;
    private boolean isQA;

    public Long getTbId() { return tbId; }
    public void setTbId(Long tbId) { this.tbId = tbId; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public boolean isReadAccess() { return readAccess; }
    public void setReadAccess(boolean readAccess) { this.readAccess = readAccess; }
    
    public boolean isWriteAccess() { return writeAccess; }
    public void setWriteAccess(boolean writeAccess) { this.writeAccess = writeAccess; }
    
    public boolean isQA() { return isQA; }
    public void setQA(boolean isQA) { this.isQA = isQA; }
}
