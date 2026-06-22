package com.tms.backend.job;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_checkin_history")
public class JobCheckinHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @Column(name = "version_major")
    private int versionMajor;

    @Column(name = "version_minor")
    private int versionMinor;

    @Column(name = "checked_in_by_uid")
    private String checkedInByUid;

    @Column(name = "checked_in_by_name")
    private String checkedInByName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "checked_in_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime checkedInAt;

    public JobCheckinHistory() {}

    public JobCheckinHistory(Job job, int versionMajor, int versionMinor, String checkedInByUid, String checkedInByName, String filePath, String fileName) {
        this.job = job;
        this.versionMajor = versionMajor;
        this.versionMinor = versionMinor;
        this.checkedInByUid = checkedInByUid;
        this.checkedInByName = checkedInByName;
        this.filePath = filePath;
        this.fileName = fileName;
    }

    public Long getId() { return id; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public int getVersionMajor() { return versionMajor; }
    public void setVersionMajor(int versionMajor) { this.versionMajor = versionMajor; }

    public int getVersionMinor() { return versionMinor; }
    public void setVersionMinor(int versionMinor) { this.versionMinor = versionMinor; }

    public String getCheckedInByUid() { return checkedInByUid; }
    public void setCheckedInByUid(String checkedInByUid) { this.checkedInByUid = checkedInByUid; }

    public String getCheckedInByName() { return checkedInByName; }
    public void setCheckedInByName(String checkedInByName) { this.checkedInByName = checkedInByName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(LocalDateTime checkedInAt) { this.checkedInAt = checkedInAt; }
}
