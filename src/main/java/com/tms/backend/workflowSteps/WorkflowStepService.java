package com.tms.backend.workflowSteps;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class WorkflowStepService {
    private final WorkflowStepRepository wfRepo;

    public WorkflowStepService(WorkflowStepRepository wfRepo) {
        this.wfRepo = wfRepo;
    }

    public List<WorkflowStep> getAllSteps() {
        return wfRepo.findAll();
    }
}
