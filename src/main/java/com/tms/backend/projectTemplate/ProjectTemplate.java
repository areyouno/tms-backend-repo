package com.tms.backend.projectTemplate;

import java.util.Set;

import com.tms.backend.businessUnit.BusinessUnit;
import com.tms.backend.client.Client;
import com.tms.backend.costCenter.CostCenter;
import com.tms.backend.domain.Domain;
import com.tms.backend.subDomain.SubDomain;
import com.tms.backend.user.User;
import com.tms.backend.vendor.Vendor;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "project_templates")
public class ProjectTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "source_lang")
    private String sourceLang;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "project_template_target_langs",
        joinColumns = @JoinColumn(name = "template_id")
    )
    @Column(name = "target_lang")
    private Set<String> targetLang;

    @Column(name = "machine_translation_id")
    private Long machineTranslationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_unit_id")
    private BusinessUnit businessUnit;

    @Column(name = "type")
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id")
    private Domain domain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subdomain_id")
    private SubDomain subdomain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "project_template_workflow_steps",
        joinColumns = @JoinColumn(name = "template_id")
    )
    @Column(name = "workflow_step_id")
    private Set<Long> workflowSteps;

    @Embedded
    private TemplateStatusAutomationSetting statusAutomationSetting = new TemplateStatusAutomationSetting();

    @Column(name = "note")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public User getOwner() {  return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public String getSourceLang() { return sourceLang; }
    public void setSourceLang(String sourceLang) { this.sourceLang = sourceLang; }

    public Set<String> getTargetLang() { return targetLang; }
    public void setTargetLang(Set<String> targetLang) { this.targetLang = targetLang; }

    public Long getMachineTranslationId() { return machineTranslationId; }
    public void setMachineTranslationId(Long machineTranslationId) { this.machineTranslationId = machineTranslationId; }

    public BusinessUnit getBusinessUnit() { return businessUnit; }
    public void setBusinessUnit(BusinessUnit businessUnitId) { this.businessUnit = businessUnitId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public CostCenter getCostCenter() { return costCenter; }
    public void setCostCenter(CostCenter costCenter) { this.costCenter = costCenter; }

    public Domain getDomain() { return domain; }
    public void setDomain(Domain domain) { this.domain = domain; }

    public SubDomain getSubdomain() { return subdomain; }
    public void setSubdomain(SubDomain subdomain) { this.subdomain = subdomain; }

    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }

    public Set<Long> getWorkflowSteps() { return workflowSteps; }
    public void setWorkflowSteps(Set<Long> workflowSteps) { this.workflowSteps = workflowSteps; }

    public TemplateStatusAutomationSetting getStatusAutomationSetting() { return statusAutomationSetting; }
    public void setStatusAutomationSetting(TemplateStatusAutomationSetting statusAutomationSetting) { this.statusAutomationSetting = statusAutomationSetting; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
}
