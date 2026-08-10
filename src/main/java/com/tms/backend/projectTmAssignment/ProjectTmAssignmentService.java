package com.tms.backend.projectTmAssignment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.tms.backend.dto.ProjectTmAssignmentDTO;
import com.tms.backend.dto.ProjectTmAssignmentRequest;
import com.tms.backend.dto.TmxCopyDTO;
import com.tms.backend.project.Project;
import com.tms.backend.project.ProjectRepository;
import com.tms.backend.translationMemory.TranslationMemoryService;
import com.tms.backend.workflowSteps.WorkflowStep;
import com.tms.backend.workflowSteps.WorkflowStepRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class ProjectTmAssignmentService {

    private ProjectRepository projectRepo;
    private WorkflowStepRepository wfRepo;
    private ProjectTmAssignmentRepository tmAssignmentRepo;
    private TranslationMemoryService tmService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public ProjectTmAssignmentService(
        ProjectRepository projectRepo,
        WorkflowStepRepository wfRepo,
        ProjectTmAssignmentRepository tmAssignmentRepo,
        TranslationMemoryService tmService
    ){
        this.projectRepo = projectRepo;
        this.wfRepo = wfRepo;
        this.tmAssignmentRepo = tmAssignmentRepo;
        this.tmService = tmService;
    }


    @Transactional
    public List<ProjectTmAssignmentDTO> assignTMs(Long projectId, ProjectTmAssignmentRequest req) {
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
                deleteTmxCopy(existing.getTmxFilePath());
                project.getTmAssignments().remove(existing);
                tmAssignmentRepo.delete(existing);
            }
        }

        String projectFolderName = buildProjectFolderName(project);
        // Downloaded TMX bytes are cached per tmId so a wildcard assignment (one TM fanned out to
        // every workflow step) only hits the Tomato export API once per request.
        Map<Long, byte[]> tmxBytesCache = new HashMap<>();

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

            // Every workflow step gets its own physical TMX copy. A step that doesn't have one yet
            // (newly assigned, or pre-dating this feature) gets one created from the source TM.
            if (assignment.getTmxFilePath() == null) {
                byte[] tmxBytes = tmxBytesCache.computeIfAbsent(tmDto.tmId(), tmService::exportTmx);
                copyTmxForStep(assignment, projectFolderName, tmxBytes);
            }

            savedAssignments.add(assignment);
        }

        // Convert to DTOs and return
        return savedAssignments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private void copyTmxForStep(ProjectTmAssignment assignment, String projectFolderName, byte[] tmxBytes) {
        try {
            Path baseDir = Paths.get(uploadDir);
            String stepFolderName = "step-" + assignment.getWorkflowStep().getId()
                    + "-" + sanitizeForPath(assignment.getWorkflowStep().getName());
            Path targetDir = baseDir.resolve("projects").resolve(projectFolderName)
                    .resolve("tmx").resolve(stepFolderName);
            Files.createDirectories(targetDir);

            Path targetFile = targetDir.resolve("tm-" + assignment.getTmId() + ".tmx");
            Files.write(targetFile, tmxBytes);

            assignment.setTmxFilePath(baseDir.relativize(targetFile).toString());
            assignment.setTmxFileSizeBytes((long) tmxBytes.length);
            assignment.setTmxCopiedAt(LocalDateTime.now());
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to copy TMX file for TM " + assignment.getTmId()
                            + " to workflow step " + assignment.getWorkflowStep().getId(), e);
        }
    }

    private void deleteTmxCopy(String relativeTmxFilePath) {
        if (relativeTmxFilePath == null) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(uploadDir).resolve(relativeTmxFilePath));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete TMX copy at " + relativeTmxFilePath, e);
        }
    }

    private String buildProjectFolderName(Project project) {
        return project.getId() + "-" + sanitizeForPath(project.getName());
    }

    private static String sanitizeForPath(String input) {
        if (input == null || input.isBlank()) {
            return "untitled";
        }
        String sanitized = input.trim().replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_");
        return sanitized.isEmpty() ? "untitled" : sanitized;
    }

    public List<TmxCopyDTO> getTmxFileByWorkflowStep(Long projectId, Long workflowStepId) {
        return tmAssignmentRepo.findByProjectIdAndWorkflowStepId(projectId, workflowStepId).stream()
                .filter(a -> a.getTmxFilePath() != null)
                .map(a -> new TmxCopyDTO(
                        a.getTmId(),
                        a.getWorkflowStep().getId(),
                        a.getTmxFilePath(),
                        a.getTmxFileSizeBytes(),
                        a.getTmxCopiedAt()))
                .collect(Collectors.toList());
    }

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
