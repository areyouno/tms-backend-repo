package com.tms.backend.translationMemory;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class TmxImportJob {

    @Id
    private String jobId;
    private Long tmId;
    private String userName;
    private String fileName;
    private String status;
    private Double progressPercent;
    private Integer processedCount;
    private Integer totalCount;
    private Integer importedCount;
    private Integer skippedCount;
    private Integer languageMismatchSkippedCount;
    private Integer emptySegmentSkippedCount;
    private Integer duplicateSkippedCount;
    private Integer invalidTuSkippedCount;
    private Integer overwrittenCount;
    private Boolean isCompleted;
    private Boolean isCancelled;
    private Boolean wasDiscarded;
    private Integer discardedCount;
    private Boolean keepPartialResults;
    private String errorMessage;
    private String startedAt;
    private String completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TmxImportJob pending(String jobId, Long tmId, String userName) {
        TmxImportJob job = new TmxImportJob();
        job.jobId = jobId;
        job.tmId = tmId;
        job.userName = userName;
        job.status = "pending";
        job.createdAt = LocalDateTime.now();
        job.updatedAt = LocalDateTime.now();
        return job;
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public Long getTmId() { return tmId; }
    public void setTmId(Long tmId) { this.tmId = tmId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Double progressPercent) { this.progressPercent = progressPercent; }
    public Integer getProcessedCount() { return processedCount; }
    public void setProcessedCount(Integer processedCount) { this.processedCount = processedCount; }
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public Integer getImportedCount() { return importedCount; }
    public void setImportedCount(Integer importedCount) { this.importedCount = importedCount; }
    public Integer getSkippedCount() { return skippedCount; }
    public void setSkippedCount(Integer skippedCount) { this.skippedCount = skippedCount; }
    public Integer getLanguageMismatchSkippedCount() { return languageMismatchSkippedCount; }
    public void setLanguageMismatchSkippedCount(Integer languageMismatchSkippedCount) { this.languageMismatchSkippedCount = languageMismatchSkippedCount; }
    public Integer getEmptySegmentSkippedCount() { return emptySegmentSkippedCount; }
    public void setEmptySegmentSkippedCount(Integer emptySegmentSkippedCount) { this.emptySegmentSkippedCount = emptySegmentSkippedCount; }
    public Integer getDuplicateSkippedCount() { return duplicateSkippedCount; }
    public void setDuplicateSkippedCount(Integer duplicateSkippedCount) { this.duplicateSkippedCount = duplicateSkippedCount; }
    public Integer getInvalidTuSkippedCount() { return invalidTuSkippedCount; }
    public void setInvalidTuSkippedCount(Integer invalidTuSkippedCount) { this.invalidTuSkippedCount = invalidTuSkippedCount; }
    public Integer getOverwrittenCount() { return overwrittenCount; }
    public void setOverwrittenCount(Integer overwrittenCount) { this.overwrittenCount = overwrittenCount; }
    public Boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(Boolean isCompleted) { this.isCompleted = isCompleted; }
    public Boolean getIsCancelled() { return isCancelled; }
    public void setIsCancelled(Boolean isCancelled) { this.isCancelled = isCancelled; }
    public Boolean getWasDiscarded() { return wasDiscarded; }
    public void setWasDiscarded(Boolean wasDiscarded) { this.wasDiscarded = wasDiscarded; }
    public Integer getDiscardedCount() { return discardedCount; }
    public void setDiscardedCount(Integer discardedCount) { this.discardedCount = discardedCount; }
    public Boolean getKeepPartialResults() { return keepPartialResults; }
    public void setKeepPartialResults(Boolean keepPartialResults) { this.keepPartialResults = keepPartialResults; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }
    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
