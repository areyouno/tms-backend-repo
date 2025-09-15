package com.tms.backend.workflowSteps;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.WorkflowStepDTO;

@RestController
@RequestMapping("/workflow-steps")
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

    private WorkflowStepDTO convertToDTO(WorkflowStep step) {
        return new WorkflowStepDTO(
            step.getId(),
            step.getName(),
            step.getDisplayOrder()
        );
    }
}
