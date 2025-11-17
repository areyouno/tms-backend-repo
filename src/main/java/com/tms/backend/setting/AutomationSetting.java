package com.tms.backend.setting;

import com.tms.backend.user.User;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "automation_settings")
public class AutomationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Each user has one automation setting
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Embedded
    private UserAutomationRules userAutomationRules = new UserAutomationRules();

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public UserAutomationRules getUserAutomationRules() { return userAutomationRules; }
    public void setUserAutomationRules(UserAutomationRules userAutomationRules) { this.userAutomationRules = userAutomationRules; }
}
