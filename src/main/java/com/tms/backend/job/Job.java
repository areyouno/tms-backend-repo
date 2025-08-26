package com.tms.backend.job;

import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Set;

import com.tms.backend.project.Project;
import com.tms.backend.user.User;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;

@Entity
@Table(name = "jobs")
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // private Integer number;
    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;
    private Double confirmPct;
    private String status;
    @ElementCollection
    @CollectionTable(name = "job_target_languages", 
                    joinColumns = @JoinColumn(name = "job_id"))
    private Set<String> targetLangs;
    private String provider;
    // private String sourcePath; //when saving to local
    private LocalDateTime dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_owner_id", referencedColumnName = "user_id")
    private User jobOwner;

    //file
    private String fileName;
    private String contentType;
    @Lob
    @Column(name = "file", columnDefinition = "LONGBLOB")
    private byte[] data;
    //or if stored in cloud
    // private String filePath; // e.g., "/uploads/docs/myfile.pdf" or
    // "https://s3.amazonaws.com/bucket/file.pdf"

    public void setProject(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public Job() {}

    public Job(String filename, String contentType, byte[] data){
            this.fileName = filename;
            this.contentType = contentType;
            this.data = data;
    }

    public Long getId() {
        return id;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Double getConfirmPct() {
        return confirmPct;
    }

    public void setConfirmPct(Double confirmPct) {
        this.confirmPct = confirmPct;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Set<String> getTargetLangs() {
        return targetLangs;
    }

    public void setTargetLangs(Set<String> targetLangs) {
        this.targetLangs = targetLangs;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public User getJobOwner() {
        return jobOwner;
    }

    public void setJobOwner(User jobOwner) {
        this.jobOwner = jobOwner;
    }
}
