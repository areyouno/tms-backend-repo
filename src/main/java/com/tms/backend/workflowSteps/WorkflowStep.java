package com.tms.backend.workflowSteps;

import java.util.ArrayList;
import java.util.List;

import com.tms.backend.netRateScheme.WorkflowStepRate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class WorkflowStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Integer displayOrder;
    private String abbreviation;
    private Boolean isLQA;

    // add relation to workflowsteprate
    @OneToMany(
        mappedBy = "workflowStep",          // link back to workflowStep in WorkflowStepRate
        cascade = CascadeType.REMOVE,       // deleting this step deletes all linked rates
        orphanRemoval = true
    )
    private List<WorkflowStepRate> workflowStepRates = new ArrayList<>();
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public String getAbbreviation() { return abbreviation; }
    public void setAbbreviation(String abbreviation) { this.abbreviation = abbreviation; }
    
    public Boolean getIsLQA() { return isLQA; }
    public void setIsLQA(Boolean isLQA) { this.isLQA = isLQA; }

    public List<WorkflowStepRate> getWorkflowStepRates() { return workflowStepRates; }
    public void setWorkflowStepRates(List<WorkflowStepRate> workflowStepRates) { this.workflowStepRates = workflowStepRates; }
}
