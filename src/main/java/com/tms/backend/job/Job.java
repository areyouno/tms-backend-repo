package com.tms.backend.job;

import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

import com.tms.backend.project.Project;
import com.tms.backend.user.User;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;

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
    private String sourceLang;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_target_languages", 
                    joinColumns = @JoinColumn(name = "job_id"))
    private Set<String> targetLangs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_owner_id", referencedColumnName = "user_id")
    private User jobOwner;
    
    //file
    private String fileName;
    private String contentType;
    private Long fileSize;

    //filedownload
    private String originalFileName;
    private String convertedFileName;
    private String originalFilePath;
    private String convertedFilePath;
    private String translatedFilePath;
    private LocalDateTime fileUploadedAt;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<JobWorkflowStep> workflowSteps = new HashSet<>();

    private Long segmentCount;
    private Long pageCount;
    private Long wordCount;
    private Long characterCount;
    private Long progress;

    @Column(name = "create_date", updatable = false)
    @CreationTimestamp
    private LocalDateTime createDate;
    
    private LocalDateTime completedDate;

    private boolean deleted = false;
    private LocalDateTime deletedDate;
    private String deletedBy;

    public Job() {}

    public Job(String filename, String contentType, String originalFilePath){
            this.fileName = filename;
            this.contentType = contentType;
            this.originalFilePath = originalFilePath;
    }

    public Long getId() { return id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    
    public String getConvertedFileName() { return convertedFileName; }
    public void setConvertedFileName(String convertedFileName) { this.convertedFileName = convertedFileName; }
    
    public String getOriginalFilePath() { return originalFilePath; }
    public void setOriginalFilePath(String originalFilePath) { this.originalFilePath = originalFilePath; }
    
    public String getConvertedFilePath() { return convertedFilePath; }
    public void setConvertedFilePath(String convertedFilePath) { this.convertedFilePath = convertedFilePath; }

    public String getTranslatedFilePath() { return translatedFilePath; }
    public void setTranslatedFilePath(String translatedFilePath) { this.translatedFilePath = translatedFilePath; }
    
    public LocalDateTime getFileUploadedAt() { return fileUploadedAt; }
    public void setFileUploadedAt(LocalDateTime fileUploadedAt) { this.fileUploadedAt = fileUploadedAt; }

    public Double getConfirmPct() { return confirmPct; }
    public void setConfirmPct(Double confirmPct) { this.confirmPct = confirmPct; }

    public String getSourceLang() { return sourceLang; }
    public void setSourceLang(String sourceLang) { this.sourceLang = sourceLang; }

    public Set<String> getTargetLangs() { return targetLangs; }
    public void setTargetLangs(Set<String> targetLangs) { this.targetLangs = targetLangs; }

    public User getJobOwner() { return jobOwner; }
    public void setJobOwner(User jobOwner) { this.jobOwner = jobOwner; }

    public Set<JobWorkflowStep> getWorkflowSteps() { return workflowSteps; }
    public void setWorkflowSteps(Set<JobWorkflowStep> workflowSteps) { this.workflowSteps = workflowSteps; }

    public Long getSegmentCount() { return segmentCount; }
    public void setSegmentCount(Long segmentCount) { this.segmentCount = segmentCount; }

    public Long getPageCount() { return pageCount; }
    public void setPageCount(Long pageCount) { this.pageCount = pageCount; }

    public Long getWordCount() { return wordCount; }
    public void setWordCount(Long wordCount) { this.wordCount = wordCount; }

    public Long getCharacterCount() { return characterCount; }
    public void setCharacterCount(Long characterCount) { this.characterCount = characterCount; }

    public Long getProgress() { return progress; }
    public void setProgress(Long progress) { this.progress = progress; }

    public LocalDateTime getCreateDate() { return createDate; }
    public void setCreateDate(LocalDateTime createDate) { this.createDate = createDate; }
    
    public LocalDateTime getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDateTime completedDate) { this.completedDate = completedDate; }

    public void setProject(Project project) { this.project = project; }
    public Project getProject() { return project; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public LocalDateTime getDeletedDate() { return deletedDate; }
    public void setDeletedDate(LocalDateTime deletedDate) { this.deletedDate = deletedDate; }

    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }
}
