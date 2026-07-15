package com.tms.backend.taskList;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tms.backend.dto.TaskListCreateDTO;
import com.tms.backend.dto.TaskListDTO;
import com.tms.backend.dto.TaskListSummaryDTO;
import com.tms.backend.exception.ResourceNotFoundException;
import com.tms.backend.job.Job;
import com.tms.backend.job.JobRepository;
import com.tms.backend.job.JobWorkflowStatus;
import com.tms.backend.job.JobWorkflowStep;
import com.tms.backend.job.JobWorkflowStepRepository;
import com.tms.backend.language.Language;
import com.tms.backend.language.LanguageRepository;
import com.tms.backend.user.User;
import com.tms.backend.user.UserRepository;
import com.tms.backend.workflowSteps.WorkflowStep;
import com.tms.backend.workflowSteps.WorkflowStepRepository;

@Service
public class TaskListService {

    private final TaskListRepository taskListRepo;
    private final JobRepository jobRepo;
    private final LanguageRepository languageRepo;
    private final WorkflowStepRepository workflowStepRepo;
    private final UserRepository userRepo;
    private final JobWorkflowStepRepository jobWorkflowStepRepo;

    public TaskListService(
        TaskListRepository taskListRepo,
        JobRepository jobRepo,
        LanguageRepository languageRepo,
        WorkflowStepRepository workflowStepRepo,
        UserRepository userRepo,
        JobWorkflowStepRepository jobWorkflowStepRepo) {
        this.taskListRepo = taskListRepo;
        this.jobRepo = jobRepo;
        this.languageRepo = languageRepo;
        this.workflowStepRepo = workflowStepRepo;
        this.userRepo = userRepo;
        this.jobWorkflowStepRepo = jobWorkflowStepRepo;
    }

    @Transactional
    public TaskListDTO createTaskList(TaskListCreateDTO createDTO, String creatorUid) {
        if (createDTO.jobIds() == null || createDTO.jobIds().isEmpty()) {
            throw new IllegalArgumentException("Task list must reference at least one job");
        }

        List<Job> jobs = jobRepo.findAllById(createDTO.jobIds());
        if (jobs.size() != createDTO.jobIds().size()) {
            throw new ResourceNotFoundException("One or more jobs not found for the given ids");
        }

        long distinctProjectCount = jobs.stream()
            .map(job -> job.getProject() != null ? job.getProject().getId() : null)
            .distinct()
            .count();
        if (distinctProjectCount != 1) {
            throw new IllegalArgumentException("All selected jobs must belong to the same project");
        }

        TaskList taskList = new TaskList();
        taskList.setTaskName(createDTO.taskName());
        taskList.setDescription(createDTO.description());
        taskList.setStartDate(createDTO.startDate());
        taskList.setDueDate(createDTO.dueDate());
        taskList.setJobs(new HashSet<>(jobs));

        if (createDTO.targetLangId() != null) {
            Language targetLang = languageRepo.findById(createDTO.targetLangId())
                .orElseThrow(() -> new ResourceNotFoundException("Language not found with id: " + createDTO.targetLangId()));

            List<Long> mismatchedJobIds = jobs.stream()
                .filter(job -> job.getTargetLangs() == null || !job.getTargetLangs().contains(targetLang.getRfcCode()))
                .map(Job::getId)
                .toList();
            if (!mismatchedJobIds.isEmpty()) {
                throw new IllegalArgumentException(
                    "Jobs " + mismatchedJobIds + " do not have target language " + targetLang.getRfcCode());
            }

            taskList.setTargetLang(targetLang);
        }

        if (createDTO.workflowStepId() != null) {
            WorkflowStep workflowStep = workflowStepRepo.findById(createDTO.workflowStepId())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow step not found with id: " + createDTO.workflowStepId()));
            taskList.setWorkflowStep(workflowStep);
        }

        if (createDTO.assigneeUid() != null) {
            User assignee = userRepo.findByUid(createDTO.assigneeUid())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with uid: " + createDTO.assigneeUid()));
            taskList.setAssignee(assignee);
        }

        User creator = userRepo.findByUid(creatorUid)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with uid: " + creatorUid));
        taskList.setCreatedBy(creator.getFirstName() + " " + creator.getLastName());

        TaskList saved = taskListRepo.save(taskList);
        return toDetailDto(saved);
    }

    @Transactional(readOnly = true)
    public TaskListDTO getTaskListById(Long id) {
        TaskList taskList = taskListRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task list not found with id: " + id));
        return toDetailDto(taskList);
    }

    @Transactional(readOnly = true)
    public List<TaskListSummaryDTO> getAllTaskLists(Long projectId, String targetLangCode, Long workflowStepId) {
        return taskListRepo.findByFilters(projectId, targetLangCode, workflowStepId).stream()
            .map(TaskListSummaryDTO::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskListSummaryDTO> getTaskListsByAssignee(String assigneeUid) {
        return taskListRepo.findByAssignee_UidOrderByCreateDateDesc(assigneeUid).stream()
            .map(TaskListSummaryDTO::from)
            .collect(Collectors.toList());
    }

    @Transactional
    public void deleteTaskList(Long id) {
        if (!taskListRepo.existsById(id)) {
            throw new ResourceNotFoundException("Task list not found with id: " + id);
        }
        taskListRepo.deleteById(id);
    }

    private TaskListDTO toDetailDto(TaskList taskList) {
        Map<Long, JobWorkflowStatus> statusByJobId = Map.of();
        if (taskList.getWorkflowStep() != null && !taskList.getJobs().isEmpty()) {
            List<Long> jobIds = taskList.getJobs().stream().map(Job::getId).toList();
            statusByJobId = jobWorkflowStepRepo
                .findByJob_IdInAndWorkflowStep_Id(jobIds, taskList.getWorkflowStep().getId()).stream()
                .collect(Collectors.toMap(jws -> jws.getJob().getId(), JobWorkflowStep::getStatus));
        }
        return TaskListDTO.from(taskList, statusByJobId);
    }
}
