package com.tms.backend.project;

public enum ProjectAutomationRule {
    // Assigned rules
    ASSIGNED_1("Mark project as 'assigned' once all jobs are emailed"),
    ASSIGNED_2("Mark project as 'assigned' once all jobs are accepted"),

    // Completed rules
    COMPLETED_1("Mark project as 'completed' once all jobs are completed or delivered in the last workflow step"),
    COMPLETED_2("Mark project as 'completed' once all jobs are completed in all their workflow steps"),
    COMPLETED_3("Mark project as 'completed' once all jobs are delivered in all their workflow steps"),

    // Cancelled rule
    CANCELLED("Mark project as 'cancelled' once all jobs are cancelled"),

    // Vendor-shared rule
    COMPLETED_BY_VENDOR("Mark shared project as 'completed' once completed by vendor");

    private final String description;

    ProjectAutomationRule(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
