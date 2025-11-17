package com.tms.backend.setting;

import java.util.EnumSet;
import java.util.Set;
import com.tms.backend.project.ProjectAutomationRule;
import jakarta.persistence.*;

@Embeddable
public class UserAutomationRules {
    
    @ElementCollection(fetch = FetchType.EAGER, targetClass = ProjectAutomationRule.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
        name = "user_automation_rules",
        joinColumns = @JoinColumn(name = "automation_setting_id")
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
