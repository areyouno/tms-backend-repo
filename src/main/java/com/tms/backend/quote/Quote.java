package com.tms.backend.quote;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.tms.backend.currency.Currency;
import com.tms.backend.jobAnalysis.JobAnalysis;
import com.tms.backend.netRateScheme.NetRateScheme;
import com.tms.backend.priceList.BillingUnit;
import com.tms.backend.priceList.PriceList;
import com.tms.backend.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "quote")
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private QuoteType type = QuoteType.PROVIDER;

    // Language pair
    @Column(name = "source_language")
    private String sourceLanguage;

    @Column(name = "target_language")
    private String targetLanguage;

    // Provider (linguist/vendor assigned to this quote)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", referencedColumnName = "user_id")
    private User provider;

    // Price list reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_list_id")
    private PriceList priceList;

    // Currency
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id")
    private Currency currency;

    // Billing unit (default: WORD)
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_unit")
    private BillingUnit billingUnit = BillingUnit.WORD;

    // Net rate scheme ("net rate name")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "net_rate_scheme_id")
    private NetRateScheme netRateScheme;

    // Analysis used to derive net words ("analysis name")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_analysis_id")
    private JobAnalysis jobAnalysis;

    // Per-workflow-step data (net words + price per step)
    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<QuoteWorkflowStep> workflowSteps = new ArrayList<>();

    @Column(name = "create_date", updatable = false)
    @CreationTimestamp
    private LocalDateTime createDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public QuoteType getType() { return type; }
    public void setType(QuoteType type) { this.type = type; }

    public String getSourceLanguage() { return sourceLanguage; }
    public void setSourceLanguage(String sourceLanguage) { this.sourceLanguage = sourceLanguage; }

    public String getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }

    public User getProvider() { return provider; }
    public void setProvider(User provider) { this.provider = provider; }

    public PriceList getPriceList() { return priceList; }
    public void setPriceList(PriceList priceList) { this.priceList = priceList; }

    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }

    public BillingUnit getBillingUnit() { return billingUnit; }
    public void setBillingUnit(BillingUnit billingUnit) { this.billingUnit = billingUnit; }

    public NetRateScheme getNetRateScheme() { return netRateScheme; }
    public void setNetRateScheme(NetRateScheme netRateScheme) { this.netRateScheme = netRateScheme; }

    public JobAnalysis getJobAnalysis() { return jobAnalysis; }
    public void setJobAnalysis(JobAnalysis jobAnalysis) { this.jobAnalysis = jobAnalysis; }

    public List<QuoteWorkflowStep> getWorkflowSteps() { return workflowSteps; }
    public void setWorkflowSteps(List<QuoteWorkflowStep> workflowSteps) { this.workflowSteps = workflowSteps; }

    public LocalDateTime getCreateDate() { return createDate; }
    public void setCreateDate(LocalDateTime createDate) { this.createDate = createDate; }
}
