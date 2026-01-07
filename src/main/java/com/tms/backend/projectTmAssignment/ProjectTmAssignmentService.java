package com.tms.backend.projectTmAssignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tms.backend.dto.ProjectTmAssignmentDTO;
import com.tms.backend.dto.ProjectTmAssignmentRequest;
import com.tms.backend.project.Project;
import com.tms.backend.project.ProjectRepository;
import com.tms.backend.workflowSteps.WorkflowStep;
import com.tms.backend.workflowSteps.WorkflowStepRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class ProjectTmAssignmentService {

    private ProjectRepository projectRepo;
    private WorkflowStepRepository wfRepo;
    private ProjectTmAssignmentRepository tmAssignmentRepo;

    public ProjectTmAssignmentService(
        ProjectRepository projectRepo,
        WorkflowStepRepository wfRepo,
        ProjectTmAssignmentRepository tmAssignmentRepo
    ){
        this.projectRepo = projectRepo;
        this.wfRepo = wfRepo;
        this.tmAssignmentRepo = tmAssignmentRepo;
    }


    @Transactional
    public List<ProjectTmAssignmentDTO> assignTMs(Long projectId, ProjectTmAssignmentRequest req) {
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        // Create a set of composite keys (tmId + workflowStepId) from the request
        Set<String> requestedKeys = req.tmAssignments().stream()
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
        // Add or update assignments from the request
        for (ProjectTmAssignmentDTO tmDto : req.tmAssignments()) {
            WorkflowStep step = wfRepo.findById(tmDto.workflowStepId())
                    .orElseThrow(() -> new EntityNotFoundException("Workflow step not found"));

            Optional<ProjectTmAssignment> existingAssignment = tmAssignmentRepo.findByProjectIdAndTmIdAndWorkflowStepId(
                    projectId,
                    tmDto.tmId(),
                    tmDto.workflowStepId());

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

            savedAssignments.add(assignment);
        }

        // Convert to DTOs and return
        return savedAssignments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ProjectTmAssignmentDTO convertToDTO(ProjectTmAssignment assignment) {
    return new ProjectTmAssignmentDTO(
            assignment.getTmId(),
            assignment.isReadAccess(),
            assignment.isWriteAccess(),
            assignment.getPenalty(),
            assignment.getPriorityOrder(),
            assignment.getWorkflowStep().getId()
    );
}
}
