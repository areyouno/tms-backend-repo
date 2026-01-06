package com.tms.backend.projectTmAssignment;

import java.util.Optional;

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
    public void assignTMs(Long projectId, ProjectTmAssignmentRequest req) {
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        // Loop through each TM assignment in the request
        for (ProjectTmAssignmentDTO tmDto : req.tmAssignments()) {
            WorkflowStep step = wfRepo.findById(tmDto.workflowStepId())
                    .orElseThrow(() -> new EntityNotFoundException("Workflow step not found"));

            // Check if assignment already exists
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
        }
    }
}
