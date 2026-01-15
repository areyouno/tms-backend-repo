package com.tms.backend.netRateScheme;

import java.util.ArrayList;
import java.util.List;

import com.tms.backend.workflowSteps.WorkflowStep;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class WorkflowStepRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column(nullable = false)
    // private Long workflowStepId; // reference to master WorkflowStep
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_step_id", nullable = false)
    private WorkflowStep workflowStep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "net_rate_scheme_id", nullable = false)
    private NetRateScheme netRateScheme;

    @OneToMany(
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    @JoinColumn(name = "workflow_step_rate_id")
    private List<MatchTypeRate> matchTypeRates = new ArrayList<>();

    public WorkflowStepRate() {}

    // public WorkflowStepRate(Long workflowStepId, List<MatchTypeRate> matchTypeRates) {
    //     this.workflowStepId = workflowStepId;
    //     this.matchTypeRates = matchTypeRates;
    // }
    public WorkflowStepRate(WorkflowStep workflowStep, List<MatchTypeRate> matchTypeRates) {
        this.workflowStep = workflowStep;
        this.matchTypeRates = matchTypeRates;
    }

    public WorkflowStep getWorkflowStep() { return workflowStep; }
    public void setWorkflowStep(WorkflowStep workflowStep) { this.workflowStep = workflowStep; }

    public List<MatchTypeRate> getMatchTypeRates() { return matchTypeRates; }
    public void setMatchTypeRates(List<MatchTypeRate> matchTypeRates) { this.matchTypeRates = matchTypeRates; }

    public NetRateScheme getNetRateScheme() {
        return netRateScheme;
    }

    public void setNetRateScheme(NetRateScheme netRateScheme) {
        this.netRateScheme = netRateScheme;
    }
}
