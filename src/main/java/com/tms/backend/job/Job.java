package com.tms.backend.job;

import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

import com.tms.backend.project.Project;
import com.tms.backend.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

@Entity
@Table(name = "jobs")
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;
    private Double confirmPct;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobWorkflowStatus status = JobWorkflowStatus.NEW;

    private String sourceLang;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_target_languages", 
                    joinColumns = @JoinColumn(name = "job_id"))
    private Set<String> targetLangs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", referencedColumnName = "user_id")
    private User provider;

    private LocalDateTime dueDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_owner_id", referencedColumnName = "user_id")
    private User jobOwner;
    
    //file
    private String fileName;
    private String filePath;
    private String contentType;
    private Long fileSize;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC")
    private List<JobWorkflowStep> workflowSteps = new ArrayList<>();
    
    private Long wordCount;
    private Long progress;

    @Column(name = "create_date", updatable = false)
    @CreationTimestamp
    private LocalDateTime createDate;

    public void setProject(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public Job() {}

    public Job(String filename, String contentType, String filePath){
            this.fileName = filename;
            this.contentType = contentType;
            this.filePath = filePath;
    }

    public Long getId() { return id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getFileSize() { return fileSize; }

    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Double getConfirmPct() { return confirmPct; }
    public void setConfirmPct(Double confirmPct) { this.confirmPct = confirmPct; }

    public JobWorkflowStatus getStatus() { return status; }
    public void setStatus(JobWorkflowStatus status) { this.status = status; }

    public String getSourceLang() { return sourceLang; }
    public void setSourceLang(String sourceLang) { this.sourceLang = sourceLang; }

    public Set<String> getTargetLangs() { return targetLangs; }
    public void setTargetLangs(Set<String> targetLangs) { this.targetLangs = targetLangs; }

    public User getProvider() { return provider; }
    public void setProvider(User provider) { this.provider = provider; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public User getJobOwner() { return jobOwner; }
    public void setJobOwner(User jobOwner) { this.jobOwner = jobOwner; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public List<JobWorkflowStep> getWorkflowSteps() { return workflowSteps; }
    public void setWorkflowSteps(List<JobWorkflowStep> workflowSteps) { this.workflowSteps = workflowSteps; }
    
    // Helper method to add a workflow step
    public void addWorkflowStep(JobWorkflowStep workflowStep) {
        workflowSteps.add(workflowStep);
        workflowStep.setJob(this);
    }
    
    // Helper method to remove a workflow step
    public void removeWorkflowStep(JobWorkflowStep workflowStep) {
        workflowSteps.remove(workflowStep);
        workflowStep.setJob(null);
    }

    public Long getWordCount() { return wordCount; }
    public void setWordCount(Long wordCount) { this.wordCount = wordCount; }

    public Long getProgress() { return progress; }
    public void setProgress(Long progress) { this.progress = progress; }

    public LocalDateTime getCreateDate() { return createDate; }
    public void setCreateDate(LocalDateTime createDate) { this.createDate = createDate; }
}
