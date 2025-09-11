package com.tms.backend.job;

public enum JobWorkflowStatus {
    NEW("New"),
    EMAILED("Emailed to provider"),
    ACCEPTED("Accepted by provider"),
    DECLINED("Declined to provider"),
    COMPLETED("Completed by provider"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    private final String name;

    JobWorkflowStatus(String name) {
        this.name = name;
    }

    public String getName() { return name; }
}
