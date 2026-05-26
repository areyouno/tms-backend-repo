package com.tms.backend.workflowSteps;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.tms.backend.dto.WorkflowStepCreateDTO;
import com.tms.backend.exception.ResourceNotFoundException;

import jakarta.transaction.Transactional;

@Service
public class WorkflowStepService {
    private final WorkflowStepRepository wfRepo;

    public WorkflowStepService(WorkflowStepRepository wfRepo) {
        this.wfRepo = wfRepo;
    }

    public List<WorkflowStep> getAllSteps() {
        return wfRepo.findAll();
    }

    @Transactional
    public WorkflowStep createWorkflowStep(WorkflowStepCreateDTO createDTO) {
        if (wfRepo.existsByName(createDTO.name())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Workflow step with name '" + createDTO.name() + "' already exists");
        }

        if (wfRepo.existsByAbbreviation(createDTO.abbreviation())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Workflow step with abbreviation '" + createDTO.abbreviation() + "' already exists");
        }

        if (wfRepo.existsByDisplayOrder(createDTO.displayOrder())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Workflow step with display order " + createDTO.displayOrder() + " already exists");
        }

        WorkflowStep workflowStep = new WorkflowStep();
        workflowStep.setName(createDTO.name());
        workflowStep.setAbbreviation(createDTO.abbreviation());
        workflowStep.setDisplayOrder(createDTO.displayOrder());
        workflowStep.setIsLQA(createDTO.isLQA() != null ? createDTO.isLQA() : false);

        return wfRepo.save(workflowStep);
    }

    public WorkflowStep updateWorkflowStep(Long id, WorkflowStepCreateDTO updateDTO) {
        WorkflowStep existingStep = wfRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow step not found with id: " + id));

        existingStep.setName(updateDTO.name());
        existingStep.setDisplayOrder(updateDTO.displayOrder());
        existingStep.setAbbreviation(updateDTO.abbreviation());
        existingStep.setIsLQA(updateDTO.isLQA());

        return wfRepo.save(existingStep);
    }

    @Transactional
    public String deleteWorkflowStep(Long id) {
        WorkflowStep step = wfRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Workflow step with ID " + id + " not found"));

        String stepName = step.getName();
        wfRepo.deleteById(id);
        return stepName;
    }

    @Transactional
    public void deleteWorkflowSteps(List<Long> id) {
        List<WorkflowStep> steps = wfRepo.findAllById(id);

        if (steps.isEmpty()) {
            throw new RuntimeException("No steps found for the given IDs");
        }

        wfRepo.deleteAll(steps);
    }
}
