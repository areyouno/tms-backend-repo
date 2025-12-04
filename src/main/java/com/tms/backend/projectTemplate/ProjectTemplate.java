package com.tms.backend.projectTemplate;

import java.util.Set;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "project_templates")
public class ProjectTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String projectName;
    
    private Long userId;

    private Long ownerId;

    private String sourceLang;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_template_target_langs", joinColumns = @JoinColumn(name = "template_id"))
    @Column(name = "target_lang")
    private Set<String> targetLang;

    private Long machineTranslationId;

    private Long businessUnitId;

    private String type;

    private Long clientId;

    private Long costCenterId;

    private Long domainId;

    private Long subdomainId;

    private Long vendorId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_template_workflow_steps", joinColumns = @JoinColumn(name = "template_id"))
    @Column(name = "workflow_step_id")
    private Set<Long> workflowSteps;

    @Embedded
    private TemplateStatusAutomationSetting statusAutomationSetting = new TemplateStatusAutomationSetting();

    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getOwnerId() {  return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getSourceLang() { return sourceLang; }
    public void setSourceLang(String sourceLang) { this.sourceLang = sourceLang; }

    public Set<String> getTargetLang() { return targetLang; }
    public void setTargetLang(Set<String> targetLang) { this.targetLang = targetLang; }

    public Long getMachineTranslationId() { return machineTranslationId; }
    public void setMachineTranslationId(Long machineTranslationId) { this.machineTranslationId = machineTranslationId; }

    public Long getBusinessUnitId() { return businessUnitId; }
    public void setBusinessUnitId(Long businessUnitId) { this.businessUnitId = businessUnitId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getCostCenterId() { return costCenterId; }
    public void setCostCenterId(Long costCenterId) { this.costCenterId = costCenterId; }

    public Long getDomainId() { return domainId; }
    public void setDomainId(Long domainId) { this.domainId = domainId; }

    public Long getSubdomainId() { return subdomainId; }
    public void setSubdomainId(Long subdomainId) { this.subdomainId = subdomainId; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public Set<Long> getWorkflowSteps() { return workflowSteps; }
    public void setWorkflowSteps(Set<Long> workflowSteps) { this.workflowSteps = workflowSteps; }

    public TemplateStatusAutomationSetting getStatusAutomationSetting() { return statusAutomationSetting; }
    public void setStatusAutomationSetting(TemplateStatusAutomationSetting statusAutomationSetting) { this.statusAutomationSetting = statusAutomationSetting; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
