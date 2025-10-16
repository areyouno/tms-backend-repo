package com.tms.backend.project;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tms.backend.businessUnit.BusinessUnit;
import com.tms.backend.client.Client;
import com.tms.backend.costCenter.CostCenter;
import com.tms.backend.domain.Domain;
import com.tms.backend.job.Job;
import com.tms.backend.machineTranslation.MachineTranslation;
import com.tms.backend.subDomain.SubDomain;
import com.tms.backend.user.User;
import com.tms.backend.workflowSteps.WorkflowStep;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

@Table(name = "projects")
@Entity
public class Project {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private String createdBy;
    private LocalDateTime createDate;

    @Column(columnDefinition = "VARCHAR(20)")
    private String status;
    
    private LocalDateTime dueDate;

    @Column(columnDefinition = "VARCHAR(10)")
    private String sourceLang;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_target_languages", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "language_code", length = 10) 
    private Set<String> targetLanguages;

    @ManyToOne
    @JoinColumn(name = "machine_trans_id", referencedColumnName = "id")
    private MachineTranslation machineTranslation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private User owner;

    @ManyToOne
    @JoinColumn(name = "business_unit_id", referencedColumnName = "id")
    private BusinessUnit businessUnit;

    private String purchaseOrderNum;
    
    @Column(columnDefinition = "VARCHAR(20)")
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Size(max = 200, message = "Note cannot exceed 200 characters")
    @Column(columnDefinition = "TEXT")
    private String note;

    @ManyToOne
    @JoinColumn(name = "cost_center_id", referencedColumnName = "id")
    private CostCenter costCenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false)
    private Domain domain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subdomain_id", nullable = false)
    private SubDomain subdomain;

    @ManyToMany
    @JoinTable(
        name = "project_workflow_step",
        joinColumns = @JoinColumn(name = "project_id"),
        inverseJoinColumns = @JoinColumn(name = "workflow_step_id")
    )
    private Set<WorkflowStep> workflowSteps = new HashSet<>();

    Boolean fileHandover;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Job> jobs = new ArrayList<>();

    BigDecimal progress;

    private boolean deleted = false;
    private LocalDateTime deletedDate;
    private String deletedBy;
    private LocalDateTime lastViewDate;

    public void addJob(Job job) {
        jobs.add(job);
        job.setProject(this);
    }

    public void deleteJob(Job job) {
        jobs.remove(job); // removes it from the collection
        job.setProject(null); // marks it as orphaned
    }

    public List<Job> getJobs() { return jobs; }
    public void setJobs(List<Job> jobs) { this.jobs = jobs; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreateDate() { return createDate; }
    public void setCreateDate(LocalDateTime createDate) { this.createDate = createDate;}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public String getSourceLang() { return sourceLang; }
    public void setSourceLang(String sourceLang) { this.sourceLang = sourceLang; }

    public Set<String> getTargetLanguages() { return targetLanguages; }
    public void setTargetLanguages(Set<String> targetLanguages) { this.targetLanguages = targetLanguages; }

    public MachineTranslation getMachineTranslation() { return machineTranslation; }
    public void setMachineTranslation(MachineTranslation machineTranslation) { this.machineTranslation = machineTranslation; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public BusinessUnit getBusinessUnit() { return businessUnit; }
    public void setBusinessUnit(BusinessUnit businessUnit) { this.businessUnit = businessUnit; }

    public String getPurchaseOrderNum() { return purchaseOrderNum; }
    public void setPurchaseOrderNum(String purchaseOrder) { this.purchaseOrderNum = purchaseOrder; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public CostCenter getCostCenter() { return costCenter; }
    public void setCostCenter(CostCenter costCenter) { this.costCenter = costCenter; }

    public Domain getDomain() { return domain; }
    public void setDomain(Domain domain) { this.domain = domain; }

    public SubDomain getSubdomain() { return subdomain; }
    public void setSubdomain(SubDomain subdomain) { this.subdomain = subdomain; }

    public Set<WorkflowStep> getWorkflowSteps() { return workflowSteps; }
    public void setWorkflowSteps(Set<WorkflowStep> workflowSteps) { this.workflowSteps = workflowSteps; }

    public Boolean getFileHandover() { return fileHandover; }
    public void setFileHandover(Boolean fileHandover) { this.fileHandover = fileHandover; }

    public BigDecimal getProgress() { return progress; }
    public void setProgress(BigDecimal progress) { this.progress = progress; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public LocalDateTime getDeletedDate() { return deletedDate; }
    public void setDeletedDate(LocalDateTime deletedDate) { this.deletedDate = deletedDate; }

    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }

    public LocalDateTime getLastViewDate() { return lastViewDate; }
    public void setLastViewDate(LocalDateTime lastViewDate) { this.lastViewDate = lastViewDate; }

}