package com.tms.backend.priceList;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tms.backend.workflowSteps.WorkflowStep;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "price_list_wf_step_price")
public class PriceListWorkflowStepPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_pair_id", nullable = false)
    @JsonIgnore
    private PriceListLanguagePair languagePair;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "workflow_step_id", nullable = false)
    private WorkflowStep workflowStep;

    private Double price = 0.0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PriceListLanguagePair getLanguagePair() { return languagePair; }
    public void setLanguagePair(PriceListLanguagePair languagePair) { this.languagePair = languagePair; }

    public WorkflowStep getWorkflowStep() { return workflowStep; }
    public void setWorkflowStep(WorkflowStep workflowStep) { this.workflowStep = workflowStep; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
