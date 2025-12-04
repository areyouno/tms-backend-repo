package com.tms.backend.projectTemplate;

import java.util.EnumSet;
import java.util.Set;

import com.tms.backend.project.ProjectAutomationRule;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;

@Embeddable
public class TemplateStatusAutomationSetting {
    
    @ElementCollection(fetch = FetchType.EAGER, targetClass = ProjectAutomationRule.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
        name = "project_template_status_automation_rules",
        joinColumns = @JoinColumn(name = "template_id")
    )
    @Column(name = "rule_name")
    private Set<ProjectAutomationRule> enabledRules = EnumSet.noneOf(ProjectAutomationRule.class);

    public Set<ProjectAutomationRule> getEnabledRules() {
        return enabledRules;
    }

    public void setEnabledRules(Set<ProjectAutomationRule> enabledRules) {
        this.enabledRules = enabledRules;
    }

    public boolean hasRule(ProjectAutomationRule rule) {
        return enabledRules != null && enabledRules.contains(rule);
    }
}
