package com.tms.backend.workflowSteps;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.WorkflowStepCreateDTO;
import com.tms.backend.dto.WorkflowStepDTO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/workflow-steps")
public class WorkflowStepController {
    private final WorkflowStepService wfService;

    public WorkflowStepController(WorkflowStepService wfService){
        this.wfService = wfService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<WorkflowStepDTO> getAllSteps() {
        return wfService.getAllSteps()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('administrator')")  // Only admins can create workflow steps
    public ResponseEntity<WorkflowStepDTO> createWorkflowStep(@Valid @RequestBody WorkflowStepCreateDTO createDTO) {
        WorkflowStep created = wfService.createWorkflowStep(createDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(created));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('administrator')")  // Only admins can delete workflow steps
    public ResponseEntity<String> deleteWorkflowStep(@PathVariable Long id) {
        wfService.deleteWorkflowStep(id);
        return ResponseEntity.ok("Workflow step deleted successfully");
    }

    private WorkflowStepDTO convertToDTO(WorkflowStep step) {
        return new WorkflowStepDTO(
            step.getId(),
            step.getName(),
            step.getDisplayOrder(),
            step.getAbbreviation(),
            step.getIsLQA()
        );
    }
}
