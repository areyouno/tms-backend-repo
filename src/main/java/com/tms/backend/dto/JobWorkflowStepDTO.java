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
    JobWorkflowStatus status,
    Integer stepOrder
) {
    public static JobWorkflowStepDTO from(JobWorkflowStep wfStep) {
        User provider = wfStep.getProvider();
        User notifyUser = wfStep.getNotifyUser();
        
        return new JobWorkflowStepDTO(
            wfStep.getId(),
            provider != null ? provider.getUid() : null,
            extractUserName(provider),
            wfStep.getDueDate(),
            notifyUser != null ? notifyUser.getUid() : null,
            extractUserName(notifyUser),
            wfStep.getStatus(),
            wfStep.getWorkflowStep().getDisplayOrder()
        );
    }

    private static String extractUserName(User user) {
        if (user == null) return null;
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        return (lastName + " " + firstName).trim();
    }
}
