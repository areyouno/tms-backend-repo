package com.tms.backend.settingPreTranslation;

import com.tms.backend.user.User;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class PreTranslationSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    private boolean overwriteExistingTranslationsInTargetSegments = true;
    private boolean preTranslateOnJobCreation = true;
    private boolean preTranslateFromTranslationMem = true;
    private int tmThreshold = 100;
    private boolean preTranslateFromNonTranslatables = false;
    private boolean preTranslateFromMachineTranslation = false;

    //set segment status to 'confirmed' for
    private boolean transMemoryMatch_101Confirmed = true;
    private boolean transMemoryMatch_100Confirmed = true;
    private boolean nonTranslatables_100Confirmed = false;
    private boolean machineTranslationSuggestionsQpsConfirmed = false;
    private int qpsThreshold = 100;

    // set job to completed once
    private boolean preTranslated = false;
    private boolean preTranslatedAndAllSegmentsConfirmed = false;
    // set project to completed once
    private boolean allJobsPreTranslated = false;
    // lock segment for 
    private boolean transMemoryMatch_101Lock = false;
    private boolean transMemoryMatch_100Lock = false;
    private boolean nonTranslatables_100Lock = false;
    private boolean machineTranslationSuggestionsQpsLock = false;
    private int qpsThresholdLock = 100;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public boolean isOverwriteExistingTranslationsInTargetSegments() { return overwriteExistingTranslationsInTargetSegments; }
    public void setOverwriteExistingTranslationsInTargetSegments(boolean overwriteExistingTranslationsInTargetSegments) { this.overwriteExistingTranslationsInTargetSegments = overwriteExistingTranslationsInTargetSegments; }
    
    public boolean isPreTranslateOnJobCreation() { return preTranslateOnJobCreation; }
    public void setPreTranslateOnJobCreation(boolean preTranslateOnJobCreation) { this.preTranslateOnJobCreation = preTranslateOnJobCreation;}
    
    public boolean isPreTranslateFromTranslationMem() { return preTranslateFromTranslationMem; }
    public void setPreTranslateFromTranslationMem(boolean preTranslateFromTranslationMem) { this.preTranslateFromTranslationMem = preTranslateFromTranslationMem; }
    
    public int getTmThreshold() { return tmThreshold; }
    public void setTmThreshold(int tmThreshold) { this.tmThreshold = tmThreshold; }
    
    public boolean isPreTranslateFromNonTranslatables() { return preTranslateFromNonTranslatables; }
    public void setPreTranslateFromNonTranslatables(boolean preTranslateFromNonTranslatables) { this.preTranslateFromNonTranslatables = preTranslateFromNonTranslatables; }

    public boolean isPreTranslateFromMachineTranslation() { return preTranslateFromMachineTranslation; }
    public void setPreTranslateFromMachineTranslation(boolean preTranslateFromMachineTranslation) { this.preTranslateFromMachineTranslation = preTranslateFromMachineTranslation; }

    public boolean isTransMemoryMatch_101Confirmed() { return transMemoryMatch_101Confirmed; }
    public void setTransMemoryMatch_101Confirmed(boolean transMemoryMatch_101Confirmed) { this.transMemoryMatch_101Confirmed = transMemoryMatch_101Confirmed; }

    public boolean isTransMemoryMatch_100Confirmed() { return transMemoryMatch_100Confirmed; }
    public void setTransMemoryMatch_100Confirmed(boolean transMemoryMatch_100Confirmed) { this.transMemoryMatch_100Confirmed = transMemoryMatch_100Confirmed; }

    public boolean isNonTranslatables_100Confirmed() { return nonTranslatables_100Confirmed; }
    public void setNonTranslatables_100Confirmed(boolean nonTranslatables_100Confirmed) { this.nonTranslatables_100Confirmed = nonTranslatables_100Confirmed; }

    public boolean isMachineTranslationSuggestionsQpsConfirmed() { return machineTranslationSuggestionsQpsConfirmed; }
    public void setMachineTranslationSuggestionsQpsConfirmed(boolean machineTranslationSuggestionsQpsConfirmed) { this.machineTranslationSuggestionsQpsConfirmed = machineTranslationSuggestionsQpsConfirmed; }
    
    public int getQpsThreshold() { return qpsThreshold; }
    public void setQpsThreshold(int qpsThreshold) { this.qpsThreshold = qpsThreshold; }
    
    public boolean isPreTranslated() { return preTranslated; }
    public void setPreTranslated(boolean preTranslated) { this.preTranslated = preTranslated; }

    public boolean isPreTranslatedAndAllSegmentsConfirmed() { return preTranslatedAndAllSegmentsConfirmed; }
    public void setPreTranslatedAndAllSegmentsConfirmed(boolean preTranslatedAndAllSegmentsConfirmed) { this.preTranslatedAndAllSegmentsConfirmed = preTranslatedAndAllSegmentsConfirmed; }

    public boolean isAllJobsPreTranslated() { return allJobsPreTranslated; }
    public void setAllJobsPreTranslated(boolean allJobsPreTranslated) { this.allJobsPreTranslated = allJobsPreTranslated; }
    
    public boolean isTransMemoryMatch_101Lock() { return transMemoryMatch_101Lock; }
    public void setTransMemoryMatch_101Lock(boolean transMemoryMatch_101Lock) { this.transMemoryMatch_101Lock = transMemoryMatch_101Lock; }

    public boolean isTransMemoryMatch_100Lock() { return transMemoryMatch_100Lock; }
    public void setTransMemoryMatch_100Lock(boolean transMemoryMatch_100Lock) { this.transMemoryMatch_100Lock = transMemoryMatch_100Lock; }
    
    public boolean isNonTranslatables_100Lock() { return nonTranslatables_100Lock; }
    public void setNonTranslatables_100Lock(boolean nonTranslatables_100Lock) { this.nonTranslatables_100Lock = nonTranslatables_100Lock; }

    public boolean isMachineTranslationSuggestionsQpsLock() { return machineTranslationSuggestionsQpsLock; }
    public void setMachineTranslationSuggestionsQpsLock(boolean machineTranslationSuggestionsQpsLock) { this.machineTranslationSuggestionsQpsLock = machineTranslationSuggestionsQpsLock; }

    public int getQpsThresholdLock() { return qpsThresholdLock; }
    public void setQpsThresholdLock(int qpsThresholdLock) { this.qpsThresholdLock = qpsThresholdLock; }
}
