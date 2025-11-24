package com.tms.backend.dto;

import java.util.List;

public class DownloadJobsRequest {
    private List<Long> jobIds;
    
    public List<Long> getJobIds() {
        return jobIds;
    }
    
    public void setJobIds(List<Long> jobIds) {
        this.jobIds = jobIds;
    }
}
