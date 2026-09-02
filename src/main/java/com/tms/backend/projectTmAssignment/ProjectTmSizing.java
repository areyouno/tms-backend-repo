package com.tms.backend.projectTmAssignment;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.tms.backend.project.Project;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Tracks a Tomato template-TM sizing job submitted for one project + language pair when TMs
 * are assigned to the project (see ProjectTmSizingService). Progress is recorded here so a
 * caller can check on a still-running or completed sizing job later.
 */
@Entity
@Table(name = "project_tm_sizing")
public class ProjectTmSizing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "source_lang")
    private String sourceLang;

    @Column(name = "target_lang")
    private String targetLang;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_tm_sizing_tm_ids", joinColumns = @JoinColumn(name = "project_tm_sizing_id"))
    @Column(name = "tm_id")
    private List<Long> tmIds;

    @Column(name = "sizing_job_id")
    private String sizingJobId;

    private String status;

    @Column(name = "template_tm_name")
    private String templateTmName;

    @Column(name = "template_tm_id")
    private Long templateTmId;

    @Column(name = "template_tm_unit_count")
    private Integer templateTmUnitCount;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "requested_by_username")
    private String requestedByUsername;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getSourceLang() { return sourceLang; }
    public void setSourceLang(String sourceLang) { this.sourceLang = sourceLang; }

    public String getTargetLang() { return targetLang; }
    public void setTargetLang(String targetLang) { this.targetLang = targetLang; }

    public List<Long> getTmIds() { return tmIds; }
    public void setTmIds(List<Long> tmIds) { this.tmIds = tmIds; }

    public String getSizingJobId() { return sizingJobId; }
    public void setSizingJobId(String sizingJobId) { this.sizingJobId = sizingJobId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTemplateTmName() { return templateTmName; }
    public void setTemplateTmName(String templateTmName) { this.templateTmName = templateTmName; }

    public Long getTemplateTmId() { return templateTmId; }
    public void setTemplateTmId(Long templateTmId) { this.templateTmId = templateTmId; }

    public Integer getTemplateTmUnitCount() { return templateTmUnitCount; }
    public void setTemplateTmUnitCount(Integer templateTmUnitCount) { this.templateTmUnitCount = templateTmUnitCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getRequestedByUsername() { return requestedByUsername; }
    public void setRequestedByUsername(String requestedByUsername) { this.requestedByUsername = requestedByUsername; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
