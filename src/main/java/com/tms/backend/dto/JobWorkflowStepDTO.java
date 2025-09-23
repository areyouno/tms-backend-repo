package com.tms.backend.dto;

import java.time.LocalDateTime;

import com.tms.backend.job.JobWorkflowStatus;
import com.tms.backend.job.JobWorkflowStep;
import com.tms.backend.user.User;

public record JobWorkflowStepDTO(
    Long workflowStepId,
    String providerUid,
    String providerName,
    LocalDateTime dueDate,
    String notifyUserUid,
    String notifyUserName,
    JobWorkflowStatus status
) {
    public static JobWorkflowStepDTO from(JobWorkflowStep wfStep) {
        return new JobWorkflowStepDTO(
            wfStep.getWorkflowStep().getId(),
            wfStep.getProvider().getUid(),
            extractUserName(wfStep.getProvider()),
            wfStep.getDueDate(),
            wfStep.getNotifyUser().getUid(),
            extractUserName(wfStep.getNotifyUser()),
            wfStep.getStatus()
        );
    }

    private static String extractUserName(User user) {
        if (user == null) return null;
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        return (lastName + " " + firstName).trim();
    }
}
