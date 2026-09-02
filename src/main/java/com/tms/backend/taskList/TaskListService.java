package com.tms.backend.taskList;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tms.backend.dto.TaskListCreateDTO;
import com.tms.backend.dto.TaskListDTO;
import com.tms.backend.dto.TaskListSummaryDTO;
import com.tms.backend.dto.TmTemplateAssignResponse;
import com.tms.backend.email.EmailService;
import com.tms.backend.exception.ResourceNotFoundException;
import com.tms.backend.job.Job;
import com.tms.backend.job.JobRepository;
import com.tms.backend.job.JobWorkflowStep;
import com.tms.backend.job.JobWorkflowStepRepository;
import com.tms.backend.language.Language;
import com.tms.backend.language.LanguageRepository;
import com.tms.backend.projectTmAssignment.ProjectTmAssignment;
import com.tms.backend.projectTmAssignment.ProjectTmAssignmentRepository;
import com.tms.backend.projectTmAssignment.ProjectTmSizing;
import com.tms.backend.projectTmAssignment.ProjectTmSizingRepository;
import com.tms.backend.tomato.TomatoTmService;
import com.tms.backend.user.User;
import com.tms.backend.user.UserRepository;

@Service
public class TaskListService {

    private static final Logger log = LoggerFactory.getLogger(TaskListService.class);

    private final TaskListRepository taskListRepo;
    private final JobRepository jobRepo;
    private final LanguageRepository languageRepo;
    private final UserRepository userRepo;
    private final JobWorkflowStepRepository jobWorkflowStepRepo;
    private final EmailService emailService;
    private final TaskListRowQueryService taskListRowQueryService;
    private final ProjectTmSizingRepository tmSizingRepo;
    private final ProjectTmAssignmentRepository tmAssignmentRepo;
    private final TomatoTmService tomatoTmService;

    public TaskListService(
        TaskListRepository taskListRepo,
        JobRepository jobRepo,
        LanguageRepository languageRepo,
        UserRepository userRepo,
        JobWorkflowStepRepository jobWorkflowStepRepo,
        EmailService emailService,
        TaskListRowQueryService taskListRowQueryService,
        ProjectTmSizingRepository tmSizingRepo,
        ProjectTmAssignmentRepository tmAssignmentRepo,
        TomatoTmService tomatoTmService) {
        this.taskListRepo = taskListRepo;
        this.jobRepo = jobRepo;
        this.languageRepo = languageRepo;
        this.userRepo = userRepo;
        this.jobWorkflowStepRepo = jobWorkflowStepRepo;
        this.emailService = emailService;
        this.taskListRowQueryService = taskListRowQueryService;
        this.tmSizingRepo = tmSizingRepo;
        this.tmAssignmentRepo = tmAssignmentRepo;
        this.tomatoTmService = tomatoTmService;
    }

    @Transactional
    public TaskListDTO createTaskList(TaskListCreateDTO createDTO, String creatorUid) {
        if (createDTO.jobIds() == null || createDTO.jobIds().isEmpty()) {
            throw new IllegalArgumentException("Task list must reference at least one job");
        }

        if (createDTO.workflowStepId() == null) {
            throw new IllegalArgumentException("Task list must reference a workflow step");
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

        User assignee = null;
        if (createDTO.assigneeUid() != null) {
            assignee = userRepo.findByUid(createDTO.assigneeUid())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with uid: " + createDTO.assigneeUid()));
            taskList.setAssignee(assignee);
        }

        if (createDTO.workflowStepId() != null) {
            List<Long> jobIds = jobs.stream().map(Job::getId).toList();
            List<JobWorkflowStep> jobWorkflowSteps = jobWorkflowStepRepo
                .findByJob_IdInAndWorkflowStep_Id(jobIds, createDTO.workflowStepId());

            List<Long> jobIdsWithStep = jobWorkflowSteps.stream().map(jws -> jws.getJob().getId()).toList();
            List<Long> missingJobIds = jobIds.stream().filter(id -> !jobIdsWithStep.contains(id)).toList();
            if (!missingJobIds.isEmpty()) {
                throw new IllegalArgumentException(
                    "Jobs " + missingJobIds + " do not have workflow step with id: " + createDTO.workflowStepId());
            }

            taskList.setWorkflowStep(jobWorkflowSteps.get(0).getWorkflowStep());

            if (assignee != null) {
                for (JobWorkflowStep jobWorkflowStep : jobWorkflowSteps) {
                    jobWorkflowStep.setProvider(assignee);
                }
                jobWorkflowStepRepo.saveAll(jobWorkflowSteps);
            }
        }

        User creator = userRepo.findByUid(creatorUid)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with uid: " + creatorUid));
        taskList.setCreatedBy(creator.getFirstName() + " " + creator.getLastName());

        if (assignee != null && taskList.getWorkflowStep() != null) {
            assignPersonalTm(taskList, assignee, creator);
        }

        TaskList saved = taskListRepo.save(taskList);

        if (assignee != null && assignee.getEmail() != null && saved.getWorkflowStep() != null) {
            String sourceLangCode = saved.getJobs().stream()
                .findFirst()
                .map(Job::getSourceLang)
                .orElse(null);
            String sourceLang = sourceLangCode != null
                ? languageRepo.findByRfcCode(sourceLangCode)
                    .map(Language::getLanguageName)
                    .orElse(sourceLangCode)
                : null;

            String targetLang = saved.getTargetLang() != null
                ? saved.getTargetLang().getLanguageName()
                : saved.getJobs().stream()
                    .filter(job -> job.getTargetLangs() != null)
                    .flatMap(job -> job.getTargetLangs().stream())
                    .distinct()
                    .collect(Collectors.joining(", "));

            emailService.sendTaskAssignmentEmail(
                    assignee.getEmail(),
                    assignee.getFirstName(),
                    saved.getTaskName(),
                    saved.getWorkflowStep().getName(),
                    sourceLang,
                    targetLang,
                    saved.getStartDate(),
                    saved.getDueDate(),
                    saved.getDescription(),
                    saved.getId());
        }

        return toDetailDto(saved);
    }

    /**
     * Materializes a standby ("personal") TM for the task's assignee from the template TM
     * already sized for this project + language pair (see ProjectTmSizingService, triggered
     * from ProjectTmAssignmentService.assignTMs), and assigns it to the task's workflow step.
     * Mutates taskList in place; does not persist (caller saves it as part of task creation).
     * Failures here don't block task list creation, they're just recorded on the task list.
     */
    private void assignPersonalTm(TaskList taskList, User assignee, User creator) {
        Job primaryJob = taskList.getJobs().stream().findFirst().orElse(null);
        if (primaryJob == null || primaryJob.getProject() == null) {
            return;
        }

        Long projectId = primaryJob.getProject().getId();
        String sourceLang = primaryJob.getSourceLang();
        String targetLang = taskList.getTargetLang() != null
            ? taskList.getTargetLang().getRfcCode()
            : (primaryJob.getTargetLangs() != null
                ? primaryJob.getTargetLangs().stream().findFirst().orElse(null)
                : null);

        Optional<ProjectTmSizing> sizing = tmSizingRepo
            .findFirstByProject_IdAndSourceLangAndTargetLangAndStatusOrderByCreatedAtDesc(
                projectId, sourceLang, targetLang, "COMPLETED");

        if (sizing.isEmpty() || sizing.get().getTemplateTmId() == null) {
            log.warn("No completed TM sizing found for project {} ({} -> {}); skipping personal TM assignment",
                projectId, sourceLang, targetLang);
            taskList.setTmProvisioningStatus("SKIPPED_NO_TEMPLATE_TM");
            return;
        }

        try {
            TmTemplateAssignResponse response = tomatoTmService.assignTemplate(
                sizing.get().getTemplateTmId(), assignee.getUsername(), taskList.getWorkflowStep().getName(),
                creator.getUsername());

            taskList.setTemplateTmId(response.templateTmId());
            taskList.setAssignedTmId(response.tmId());
            taskList.setTmAssignWasExisting(response.wasExisting());
            taskList.setTmProvisioningStatus("COMPLETED");
        } catch (Exception e) {
            log.error("Failed to assign personal TM for project {} ({} -> {}): {}",
                projectId, sourceLang, targetLang, e.getMessage());
            taskList.setTmProvisioningStatus("FAILED");
            taskList.setTmProvisioningError(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public TaskListDTO getTaskListById(Long id) {
        TaskList taskList = taskListRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task list not found with id: " + id));
        return toDetailDto(taskList);
    }

    // Paginated/filtered/sorted/searchable task-list listing backing GET /api/task-lists, scoped
    // by role: admins see every task list, everyone else only their assigned ones. See
    // TaskListRowQueryService.
    @Transactional(readOnly = true)
    public Page<TaskListSummaryDTO> getAllTaskLists(User user, int page, int pageSize, String search, String sortBy,
            String sortDir, Map<String, List<String>> filters) {
        return taskListRowQueryService.findTaskLists(user, page, pageSize, search, sortBy, sortDir, filters);
    }

    @Transactional(readOnly = true)
    public List<TaskListSummaryDTO> getTaskListsByAssignee(String assigneeUid) {
        return taskListRepo.findByAssignee_UidOrderByCreateDateDesc(assigneeUid).stream()
            .map(TaskListSummaryDTO::from)
            .collect(Collectors.toList());
    }

    @Transactional
    public void deleteTaskList(Long id) {
        log.info("Deleting task list {}", id);
        TaskList taskList = taskListRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task list not found with id: " + id));

        if (List.of("SIZING", "ASSIGNING").contains(taskList.getTmProvisioningStatus())) {
            log.warn("Task list {} is being deleted while TM provisioning is still in progress (status={})",
                id, taskList.getTmProvisioningStatus());
        }

        User assignee = taskList.getAssignee();
        if (assignee != null && taskList.getWorkflowStep() != null && !taskList.getJobs().isEmpty()) {
            List<Long> jobIds = taskList.getJobs().stream().map(Job::getId).toList();
            List<JobWorkflowStep> jobWorkflowSteps = jobWorkflowStepRepo
                .findByJob_IdInAndWorkflowStep_Id(jobIds, taskList.getWorkflowStep().getId());

            List<JobWorkflowStep> assignedSteps = jobWorkflowSteps.stream()
                .filter(jws -> jws.getProvider() != null && jws.getProvider().getUid().equals(assignee.getUid()))
                .toList();
            for (JobWorkflowStep jobWorkflowStep : assignedSteps) {
                jobWorkflowStep.setProvider(null);
            }
            jobWorkflowStepRepo.saveAll(assignedSteps);
        }

        taskListRepo.deleteById(id);
        log.info("Deleted task list {}", id);

        if (assignee != null && assignee.getEmail() != null) {
            emailService.sendTaskUnassignmentEmail(assignee.getEmail(), assignee.getFirstName(), taskList.getTaskName());
        }
    }

    private TaskListDTO toDetailDto(TaskList taskList) {
        Map<Long, JobWorkflowStep> jobWorkflowStepByJobId = Map.of();
        if (taskList.getWorkflowStep() != null && !taskList.getJobs().isEmpty()) {
            List<Long> jobIds = taskList.getJobs().stream().map(Job::getId).toList();
            jobWorkflowStepByJobId = jobWorkflowStepRepo
                .findByJob_IdInAndWorkflowStep_Id(jobIds, taskList.getWorkflowStep().getId()).stream()
                .collect(Collectors.toMap(jws -> jws.getJob().getId(), jws -> jws));
        }
        return TaskListDTO.from(taskList, jobWorkflowStepByJobId, resolveMasterTmId(taskList));
    }

    /**
     * The real Tomato TM that completed translations should be merged into for this task list's
     * project + workflow step + language pair (distinct from templateTmId/assignedTmId, which
     * are scratch TMs Tomato creates per task for sizing/working purposes - see
     * assignPersonalTm above). Prefers a write-access assignment over a read-only one,
     * since some projects only have read_access configured on the intended merge target.
     */
    private Long resolveMasterTmId(TaskList taskList) {
        Job primaryJob = taskList.getJobs().stream().findFirst().orElse(null);
        if (primaryJob == null || primaryJob.getProject() == null || taskList.getWorkflowStep() == null) {
            return null;
        }

        Long projectId = primaryJob.getProject().getId();
        String sourceLang = primaryJob.getSourceLang();
        String targetLang = taskList.getTargetLang() != null
            ? taskList.getTargetLang().getRfcCode()
            : (primaryJob.getTargetLangs() != null
                ? primaryJob.getTargetLangs().stream().findFirst().orElse(null)
                : null);

        List<ProjectTmAssignment> matches = tmAssignmentRepo.findByProjectIdAndWorkflowStepIdAndSourceLangAndTargetLang(
            projectId, taskList.getWorkflowStep().getId(), sourceLang, targetLang);

        return matches.stream()
            .filter(ProjectTmAssignment::isWriteAccess)
            .findFirst()
            .or(() -> matches.stream().findFirst())
            .map(ProjectTmAssignment::getTmId)
            .orElse(null);
    }
}
