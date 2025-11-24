package com.tms.backend.dto;

import java.util.List;

public class DownloadProjectsRequest {
    private List<Long> projectIds;
    
    public List<Long> getProjectIds() {
        return projectIds;
    }
    
    public void setProjectIds(List<Long> projectIds) {
        this.projectIds = projectIds;
    }
}
