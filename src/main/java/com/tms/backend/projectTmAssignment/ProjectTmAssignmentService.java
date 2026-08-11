package com.tms.backend.projectTmAssignment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
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
import com.tms.backend.job.Job;
import com.tms.backend.job.JobWorkflowStep;
import com.tms.backend.job.JobWorkflowStepRepository;
import com.tms.backend.jobWorkflowStepTmxCopy.JobWorkflowStepTmxCopy;
import com.tms.backend.jobWorkflowStepTmxCopy.JobWorkflowStepTmxCopyRepository;
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
    private JobWorkflowStepTmxCopyRepository jobTmxCopyRepo;
    private JobWorkflowStepRepository jobWfRepo;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public ProjectTmAssignmentService(
        ProjectRepository projectRepo,
        WorkflowStepRepository wfRepo,
        ProjectTmAssignmentRepository tmAssignmentRepo,
        TranslationMemoryService tmService,
        JobWorkflowStepTmxCopyRepository jobTmxCopyRepo,
        JobWorkflowStepRepository jobWfRepo
    ){
        this.projectRepo = projectRepo;
        this.wfRepo = wfRepo;
        this.tmAssignmentRepo = tmAssignmentRepo;
        this.tmService = tmService;
        this.jobTmxCopyRepo = jobTmxCopyRepo;
        this.jobWfRepo = jobWfRepo;
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
                // Cascade: any job-level copies derived from this assignment must go first, since
                // they hold a not-null FK to it.
                List<JobWorkflowStepTmxCopy> derivedCopies = jobTmxCopyRepo.findByProjectTmAssignmentId(existing.getId());
                for (JobWorkflowStepTmxCopy derivedCopy : derivedCopies) {
                    deleteTmxCopy(derivedCopy.getTmxFilePath());
                }
                jobTmxCopyRepo.deleteAll(derivedCopies);

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

            // TM assignment and job creation can happen in either order: backfill copies onto any
            // jobs that already exist and match this assignment's workflow step + language pair.
            syncTmxCopyToExistingJobs(assignment, projectFolderName);

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

    // Job-creation trigger: for a newly created job (single, fixed language pair by this point) and
    // its freshly created JobWorkflowStep rows, back-fill job-level copies from whichever
    // ProjectTmAssignments already exist for the matching workflow step + language pair.
    public void copyTmxForJobWorkflowSteps(Job job, Collection<JobWorkflowStep> jobWorkflowSteps) {
        if (jobWorkflowSteps == null || jobWorkflowSteps.isEmpty()) {
            return;
        }
        if (job.getProject() == null || job.getSourceLang() == null
                || job.getTargetLangs() == null || job.getTargetLangs().isEmpty()) {
            return;
        }

        Long projectId = job.getProject().getId();
        String sourceLang = job.getSourceLang();
        String targetLang = job.getTargetLangs().iterator().next();
        String projectFolderName = buildProjectFolderName(job.getProject());

        for (JobWorkflowStep jws : jobWorkflowSteps) {
            List<ProjectTmAssignment> matches = tmAssignmentRepo
                    .findByProjectIdAndWorkflowStepIdAndSourceLangAndTargetLang(
                            projectId, jws.getWorkflowStep().getId(), sourceLang, targetLang);
            for (ProjectTmAssignment assignment : matches) {
                createJobTmxCopyIfMissing(job, jws, assignment, projectFolderName);
            }
        }
    }

    // TM-assignment trigger: the mirror image of the above. When a TM gets (re)assigned to a
    // project's workflow step, back-fill job-level copies onto any jobs that already exist and
    // match that assignment's workflow step + language pair.
    private void syncTmxCopyToExistingJobs(ProjectTmAssignment assignment, String projectFolderName) {
        if (assignment.getSourceLang() == null || assignment.getTargetLang() == null) {
            return;
        }

        List<JobWorkflowStep> matchingSteps = jobWfRepo
                .findByWorkflowStep_IdAndJob_Project_IdAndJob_SourceLangAndJob_DeletedFalse(
                        assignment.getWorkflowStep().getId(),
                        assignment.getProject().getId(),
                        assignment.getSourceLang());

        for (JobWorkflowStep jws : matchingSteps) {
            Job job = jws.getJob();
            if (job.getTargetLangs() == null || !job.getTargetLangs().contains(assignment.getTargetLang())) {
                continue;
            }
            createJobTmxCopyIfMissing(job, jws, assignment, projectFolderName);
        }
    }

    private void createJobTmxCopyIfMissing(Job job, JobWorkflowStep jws, ProjectTmAssignment assignment, String projectFolderName) {
        if (jobTmxCopyRepo.existsByJobWorkflowStepIdAndProjectTmAssignmentId(jws.getId(), assignment.getId())) {
            return;
        }

        try {
            Path baseDir = Paths.get(uploadDir);
            byte[] tmxBytes = assignment.getTmxFilePath() != null
                    ? Files.readAllBytes(baseDir.resolve(assignment.getTmxFilePath()))
                    : tmService.exportTmx(assignment.getTmId());

            String stepFolderName = "step-" + jws.getWorkflowStep().getId()
                    + "-" + sanitizeForPath(jws.getWorkflowStep().getName());
            Path targetDir = baseDir.resolve("projects").resolve(projectFolderName)
                    .resolve("jobs").resolve("job-" + job.getId())
                    .resolve("tmx").resolve(stepFolderName);
            Files.createDirectories(targetDir);

            Path targetFile = targetDir.resolve("tm-" + assignment.getTmId() + ".tmx");
            Files.write(targetFile, tmxBytes);

            JobWorkflowStepTmxCopy copy = new JobWorkflowStepTmxCopy();
            copy.setJobWorkflowStep(jws);
            copy.setProjectTmAssignment(assignment);
            copy.setTmxFilePath(baseDir.relativize(targetFile).toString());
            copy.setTmxFileSizeBytes((long) tmxBytes.length);
            copy.setTmxCopiedAt(LocalDateTime.now());
            jobTmxCopyRepo.save(copy);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to copy TMX file for TM " + assignment.getTmId()
                            + " to job " + job.getId() + " workflow step " + jws.getWorkflowStep().getId(), e);
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

    public List<TmxCopyDTO> getTmxFileByJobWorkflowStep(Long jobId, Long workflowStepId) {
        return jobTmxCopyRepo
                .findByJobWorkflowStep_Job_IdAndJobWorkflowStep_WorkflowStep_Id(jobId, workflowStepId).stream()
                .filter(c -> c.getTmxFilePath() != null)
                .map(c -> new TmxCopyDTO(
                        c.getProjectTmAssignment().getTmId(),
                        c.getJobWorkflowStep().getWorkflowStep().getId(),
                        c.getTmxFilePath(),
                        c.getTmxFileSizeBytes(),
                        c.getTmxCopiedAt()))
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
