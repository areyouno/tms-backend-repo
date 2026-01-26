package com.tms.backend.projectTbAssignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tms.backend.dto.ProjectTbAssignmentDTO;
import com.tms.backend.dto.ProjectTbAssignmentRequest;
import com.tms.backend.project.Project;
import com.tms.backend.project.ProjectRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class ProjectTbAssignmentService {
    private ProjectRepository projectRepo;
    private ProjectTbAssignmentRepository tbAssignmentRepo;

    public ProjectTbAssignmentService(
        ProjectRepository projectRepo,
        ProjectTbAssignmentRepository tbAssignmentRepo
    ){
        this.projectRepo = projectRepo;
        this.tbAssignmentRepo = tbAssignmentRepo;
    }

    @Transactional
    public List<ProjectTbAssignmentDTO> assignTBs(
            Long projectId,
            ProjectTbAssignmentRequest req) {
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        // Build requested TB IDs
        Set<Long> requestedTbIds = req.tbAssignments().stream()
                .map(ProjectTbAssignmentDTO::tbId)
                .collect(Collectors.toSet());

        // Remove assignments no longer present
        List<ProjectTbAssignment> existingAssignments = tbAssignmentRepo.findByProjectId(projectId);

        for (ProjectTbAssignment existing : existingAssignments) {
            if (!requestedTbIds.contains(existing.getTbId())) {
                project.getTbAssignments().remove(existing);
                tbAssignmentRepo.delete(existing);
            }
        }

        List<ProjectTbAssignment> savedAssignments = new ArrayList<>();

        // Add or update assignments
        for (ProjectTbAssignmentDTO tbDto : req.tbAssignments()) {

            Optional<ProjectTbAssignment> existingAssignment = tbAssignmentRepo.findByProjectIdAndTbId(
                    projectId,
                    tbDto.tbId());

            ProjectTbAssignment assignment;

            if (existingAssignment.isPresent()) {
                // ---- UPDATE ----
                assignment = existingAssignment.get();

            } else {
                // ---- CREATE ----
                assignment = new ProjectTbAssignment();
                assignment.setProject(project);
                assignment.setTbId(tbDto.tbId());

                project.getTbAssignments().add(assignment);
            }

            assignment.setReadAccess(tbDto.read());
            assignment.setWriteAccess(tbDto.write());
            assignment.setQA(tbDto.isQA());

            savedAssignments.add(assignment);
        }

        return savedAssignments.stream()
                .map(this::convertTbToDTO)
                .collect(Collectors.toList());
    }

    private ProjectTbAssignmentDTO convertTbToDTO(ProjectTbAssignment assignment) {
        return new ProjectTbAssignmentDTO(
                assignment.getTbId(),
                assignment.isReadAccess(),
                assignment.isWriteAccess(),
                assignment.isQA());
    }
}
