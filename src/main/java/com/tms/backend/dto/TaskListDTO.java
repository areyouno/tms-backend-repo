package com.tms.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.tms.backend.job.Job;
import com.tms.backend.job.JobWorkflowStep;
import com.tms.backend.project.Project;
import com.tms.backend.taskList.TaskList;
import com.tms.backend.user.User;

public record TaskListDTO(
    Long id,
    Long projectId,
    String projectName,
    String sourceLangCode,
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
    LocalDateTime createDate,
    List<TaskListItemDTO> items
) {
    public static TaskListDTO from(TaskList taskList, Map<Long, JobWorkflowStep> jobWorkflowStepByJobId) {
        Project project = taskList.getJobs().stream()
            .findFirst()
            .map(Job::getProject)
            .orElse(null);

        User assignee = taskList.getAssignee();

        List<TaskListItemDTO> items = taskList.getJobs().stream()
            .map(job -> TaskListItemDTO.from(job, jobWorkflowStepByJobId.get(job.getId())))
            .toList();

        return new TaskListDTO(
            taskList.getId(),
            project != null ? project.getId() : null,
            project != null ? project.getName() : null,
            project != null ? project.getSourceLang() : null,
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
                ? (assignee.isDeleted()
                    ? assignee.getLastName() + " (deleted user)"
                    : (assignee.getFirstName() + " " + assignee.getLastName()))
                : null,
            taskList.getCreatedBy(),
            taskList.getCreateDate(),
            items
        );
    }
}
