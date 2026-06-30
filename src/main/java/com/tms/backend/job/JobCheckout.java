package com.tms.backend.job;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_checkouts")
public class JobCheckout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", unique = true)
    private Job job;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @Column(name = "last_saved_at")
    private LocalDateTime lastSavedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "working_copy_path")
    private String workingCopyPath;

    @Column(name = "working_copy_file_name")
    private String workingCopyFileName;

    public JobCheckout() {}

    public JobCheckout(Job job, String userId, String userName, LocalDateTime checkedOutAt,
            LocalDateTime expiresAt, String workingCopyPath, String workingCopyFileName) {
        this.job = job;
        this.userId = userId;
        this.userName = userName;
        this.checkedOutAt = checkedOutAt;
        this.lastSavedAt = checkedOutAt;
        this.expiresAt = expiresAt;
        this.workingCopyPath = workingCopyPath;
        this.workingCopyFileName = workingCopyFileName;
    }

    public Long getId() { return id; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public LocalDateTime getCheckedOutAt() { return checkedOutAt; }
    public void setCheckedOutAt(LocalDateTime checkedOutAt) { this.checkedOutAt = checkedOutAt; }

    public LocalDateTime getLastSavedAt() { return lastSavedAt; }
    public void setLastSavedAt(LocalDateTime lastSavedAt) { this.lastSavedAt = lastSavedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getWorkingCopyPath() { return workingCopyPath; }
    public void setWorkingCopyPath(String workingCopyPath) { this.workingCopyPath = workingCopyPath; }

    public String getWorkingCopyFileName() { return workingCopyFileName; }
    public void setWorkingCopyFileName(String workingCopyFileName) { this.workingCopyFileName = workingCopyFileName; }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
}
