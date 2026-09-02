package com.tms.backend.projectTmAssignment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.tms.backend.dto.ProjectTmAssignmentDTO;
import com.tms.backend.dto.ProjectTmAssignmentRequest;
import com.tms.backend.job.Job;
import com.tms.backend.project.Project;
import com.tms.backend.project.ProjectRepository;
import com.tms.backend.workflowSteps.WorkflowStep;
import com.tms.backend.workflowSteps.WorkflowStepRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class ProjectTmAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(ProjectTmAssignmentService.class);

    private ProjectRepository projectRepo;
    private WorkflowStepRepository wfRepo;
    private ProjectTmAssignmentRepository tmAssignmentRepo;
    private ProjectTmSizingService tmSizingService;

    public ProjectTmAssignmentService(
        ProjectRepository projectRepo,
        WorkflowStepRepository wfRepo,
        ProjectTmAssignmentRepository tmAssignmentRepo,
        ProjectTmSizingService tmSizingService
    ){
        this.projectRepo = projectRepo;
        this.wfRepo = wfRepo;
        this.tmAssignmentRepo = tmAssignmentRepo;
        this.tmSizingService = tmSizingService;
    }


    @Transactional
    public List<ProjectTmAssignmentDTO> assignTMs(Long projectId, ProjectTmAssignmentRequest req,
            String requestingUsername) {
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        // Expand assignments: if workflowStepId is null, create one per project workflow step
        List<ProjectTmAssignmentDTO> expandedAssignments = new ArrayList<>();
        for (ProjectTmAssignmentDTO tmDto : req.tmAssignments()) {
            if (tmDto.workflowStepId() == null) {
                for (WorkflowStep step : project.getWorkflowSteps()) {
                    expandedAssignments.add(new ProjectTmAssignmentDTO(
                            tmDto.tmId(), tmDto.read(), tmDto.write(),
                            tmDto.penalty(), tmDto.priorityOrder(), step.getId(),
                            tmDto.sourceLang(), tmDto.targetLang()));
                }
            } else {
                expandedAssignments.add(tmDto);
            }
        }

        // Create a set of composite keys (tmId + workflowStepId) from the expanded request
        Set<String> requestedKeys = expandedAssignments.stream()
                .map(dto -> dto.tmId() + "_" + dto.workflowStepId())
                .collect(Collectors.toSet());

        // Remove assignments that are no longer in the request
        List<ProjectTmAssignment> existingAssignments = tmAssignmentRepo.findByProjectId(projectId);
        for (ProjectTmAssignment existing : existingAssignments) {
            String existingKey = existing.getTmId() + "_" + existing.getWorkflowStep().getId();
            if (!requestedKeys.contains(existingKey)) {
                project.getTmAssignments().remove(existing);
                tmAssignmentRepo.delete(existing);
            }
        }

        List<ProjectTmAssignment> savedAssignments = new ArrayList<>();
        // Add or update assignments from the expanded request
        for (ProjectTmAssignmentDTO tmDto : expandedAssignments) {
            WorkflowStep step = wfRepo.findById(tmDto.workflowStepId())
                    .orElseThrow(() -> new EntityNotFoundException("Workflow step not found"));

            Optional<ProjectTmAssignment> existingAssignment =
                    tmAssignmentRepo.findByProjectIdAndTmIdAndWorkflowStepId(
                            projectId, tmDto.tmId(), tmDto.workflowStepId());

            ProjectTmAssignment assignment;
            if (existingAssignment.isPresent()) {
                // Update existing assignment
                assignment = existingAssignment.get();
            } else {
                // Create new assignment
                assignment = new ProjectTmAssignment();
                assignment.setProject(project);
                assignment.setWorkflowStep(step);
                assignment.setTmId(tmDto.tmId());
                project.getTmAssignments().add(assignment);
            }

            // Set/update the properties
            assignment.setReadAccess(tmDto.read());
            assignment.setWriteAccess(tmDto.write());
            assignment.setPenalty(tmDto.penalty());
            assignment.setPriorityOrder(tmDto.priorityOrder());
            assignment.setSourceLang(tmDto.sourceLang());
            assignment.setTargetLang(tmDto.targetLang());

            savedAssignments.add(assignment);
        }

        triggerTmSizing(project, savedAssignments, requestingUsername);

        // Convert to DTOs and return
        return savedAssignments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Kicks off one Tomato sizing call per distinct (sourceLang, targetLang) among the just-saved
     * assignments, batching every TM that shares a language pair into a single call (Tomato's
     * tmId is repeatable in the sizing request). Runs after this transaction commits so the
     * background sizing service's own transaction is guaranteed to see the saved rows.
     */
    private void triggerTmSizing(Project project, List<ProjectTmAssignment> assignments, String requestingUsername) {
        Map<LanguagePair, List<Long>> tmIdsByPair = assignments.stream()
                .filter(a -> a.getSourceLang() != null && a.getTargetLang() != null)
                .collect(Collectors.groupingBy(
                        a -> new LanguagePair(a.getSourceLang(), a.getTargetLang()),
                        LinkedHashMap::new,
                        Collectors.mapping(ProjectTmAssignment::getTmId, Collectors.toList())));

        for (Map.Entry<LanguagePair, List<Long>> entry : tmIdsByPair.entrySet()) {
            LanguagePair pair = entry.getKey();
            List<Long> tmIds = entry.getValue().stream().distinct().collect(Collectors.toList());

            List<String> filePaths = project.getJobs().stream()
                    .filter(job -> pair.source().equals(job.getSourceLang())
                            && job.getTargetLangs() != null && job.getTargetLangs().contains(pair.target()))
                    .map(Job::getOriginalFilePath)
                    .distinct()
                    .collect(Collectors.toList());

            if (filePaths.isEmpty()) {
                log.warn("Skipping TM sizing for project {} ({} -> {}): no matching job files",
                        project.getId(), pair.source(), pair.target());
                continue;
            }

            String templateTmName = project.getName() + "-" + pair.source() + "-" + pair.target()
                    + "-" + System.currentTimeMillis();
            Long projectId = project.getId();

            Runnable submit = () -> tmSizingService.submitAndTrackSizing(
                    projectId, pair.source(), pair.target(), tmIds, filePaths, templateTmName, requestingUsername);

            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        submit.run();
                    }
                });
            } else {
                submit.run();
            }
        }
    }

    private record LanguagePair(String source, String target) {}

    private ProjectTmAssignmentDTO convertToDTO(ProjectTmAssignment assignment) {
        return new ProjectTmAssignmentDTO(
                assignment.getTmId(),
                assignment.isReadAccess(),
                assignment.isWriteAccess(),
                assignment.getPenalty(),
                assignment.getPriorityOrder(),
                assignment.getWorkflowStep().getId(),
                assignment.getSourceLang(),
                assignment.getTargetLang()
        );
    }
}
