package com.tms.backend.dto;

import org.springframework.web.multipart.MultipartFile;

public class TranslatedFileUploadRequest {
    private Long jobId;
    private MultipartFile file;
    
    public TranslatedFileUploadRequest() {}
    
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }
}
