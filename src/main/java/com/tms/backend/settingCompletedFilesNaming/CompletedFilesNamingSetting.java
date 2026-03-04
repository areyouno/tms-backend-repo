package com.tms.backend.settingCompletedFilesNaming;


import com.tms.backend.user.User;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class CompletedFilesNamingSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;
    
    private String folderName = "targetLang";
    private boolean hasNamingRule = false;
    private String namingRule = "{path}/{fileName}-{sourceLang}-{targetLang}-{workflow}-{status}";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getFolderName() { return folderName; }
    public void setFolderName(String folderName) { this.folderName = folderName; }

    public boolean isHasNamingRule() { return hasNamingRule; }
    public void setHasNamingRule(boolean hasNamingRule) { this.hasNamingRule = hasNamingRule; }

    public String getNamingRule() { return namingRule; }
    public void setNamingRule(String namingRule) { this.namingRule = namingRule; }
}
