package com.tms.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.tms.backend.taskList.TaskList;
import com.tms.backend.user.User;

public record TaskListDTO(
    Long id,
    List<Long> jobIds,
    Long projectId,
    Long targetLangId,
    String targetLangCode,
    Long workflowStepId,
    String workflowStepName,
    String taskName,
    LocalDateTime startDate,
    LocalDateTime dueDate,
    String description,
    String assigneeUid,
    String assigneeName,
    String createdBy,
    LocalDateTime createDate
) {
    public static TaskListDTO from(TaskList taskList) {
        List<Long> jobIds = taskList.getJobs().stream().map(job -> job.getId()).toList();

        Long projectId = taskList.getJobs().stream()
            .findFirst()
            .map(job -> job.getProject() != null ? job.getProject().getId() : null)
            .orElse(null);

        User assignee = taskList.getAssignee();

        return new TaskListDTO(
            taskList.getId(),
            jobIds,
            projectId,
            taskList.getTargetLang() != null ? taskList.getTargetLang().getId() : null,
            taskList.getTargetLang() != null ? taskList.getTargetLang().getRfcCode() : null,
            taskList.getWorkflowStep() != null ? taskList.getWorkflowStep().getId() : null,
            taskList.getWorkflowStep() != null ? taskList.getWorkflowStep().getName() : null,
            taskList.getTaskName(),
            taskList.getStartDate(),
            taskList.getDueDate(),
            taskList.getDescription(),
            assignee != null ? assignee.getUid() : null,
            assignee != null
                ? (assignee.isActive()
                    ? (assignee.getFirstName() + " " + assignee.getLastName())
                    : assignee.getLastName() + " (deleted user)")
                : null,
            taskList.getCreatedBy(),
            taskList.getCreateDate()
        );
    }
}
