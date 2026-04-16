package com.tms.backend.quote;

import java.math.BigDecimal;

import com.tms.backend.workflowSteps.WorkflowStep;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "quote_workflow_step")
public class QuoteWorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_step_id", nullable = false)
    private WorkflowStep workflowStep;

    // Net words derived from the linked JobAnalysis for this workflow step
    @Column(name = "net_words")
    private Long netWords;

    // Price for this specific workflow step
    @Column(name = "price", precision = 19, scale = 4)
    private BigDecimal price;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Quote getQuote() { return quote; }
    public void setQuote(Quote quote) { this.quote = quote; }

    public WorkflowStep getWorkflowStep() { return workflowStep; }
    public void setWorkflowStep(WorkflowStep workflowStep) { this.workflowStep = workflowStep; }

    public Long getNetWords() { return netWords; }
    public void setNetWords(Long netWords) { this.netWords = netWords; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
