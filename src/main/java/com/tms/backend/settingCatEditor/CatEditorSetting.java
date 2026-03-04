package com.tms.backend.settingCatEditor;

import com.tms.backend.user.User;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class CatEditorSetting {
    @Id
    @GeneratedValue
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    private boolean displayNonTranslatablesScoresInCatEditor = true;
    private boolean suggestMtOnlyForSegmentsWithTmMatchBelow = true;
    private int suggestMtOnlyPercent = 100;
    
    private boolean displayPhraseQualityPerformanceScoreMatchesInCatEditor = false;
    private boolean autoPropagateRepetitions = false;
    private boolean autoPropagateToLockedRepetitions = false;
    private boolean setSegmentStatusConfirmedForRepetitions = true;
    private boolean lockSubsequentRepetitions = false;
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public boolean isDisplayNonTranslatablesScoresInCatEditor() { return displayNonTranslatablesScoresInCatEditor; }
    public void setDisplayNonTranslatablesScoresInCatEditor(boolean displayNonTranslatablesScoresInCatEditor) { this.displayNonTranslatablesScoresInCatEditor = displayNonTranslatablesScoresInCatEditor; }

    public boolean isSuggestMtOnlyForSegmentsWithTmMatchBelow() { return suggestMtOnlyForSegmentsWithTmMatchBelow; }
    public void setSuggestMtOnlyForSegmentsWithTmMatchBelow(boolean suggestMtOnlyForSegmentsWithTmMatchBelow) { this.suggestMtOnlyForSegmentsWithTmMatchBelow = suggestMtOnlyForSegmentsWithTmMatchBelow; }

    public int getSuggestMtOnlyPercent() { return suggestMtOnlyPercent; }
    public void setSuggestMtOnlyPercent(int suggestMtOnlyPercent) { this.suggestMtOnlyPercent = suggestMtOnlyPercent; }

    public boolean isDisplayPhraseQualityPerformanceScoreMatchesInCatEditor() { return displayPhraseQualityPerformanceScoreMatchesInCatEditor; }
    public void setDisplayPhraseQualityPerformanceScoreMatchesInCatEditor(
            boolean displayPhraseQualityPerformanceScoreMatchesInCatEditor) { this.displayPhraseQualityPerformanceScoreMatchesInCatEditor = displayPhraseQualityPerformanceScoreMatchesInCatEditor; }

    public boolean isAutoPropagateRepetitions() { return autoPropagateRepetitions; }
    public void setAutoPropagateRepetitions(boolean autoPropagateRepetitions) { this.autoPropagateRepetitions = autoPropagateRepetitions; }

    public boolean isAutoPropagateToLockedRepetitions() { return autoPropagateToLockedRepetitions; }
    public void setAutoPropagateToLockedRepetitions(boolean autoPropagateToLockedRepetitions) { this.autoPropagateToLockedRepetitions = autoPropagateToLockedRepetitions; }

    public boolean isSetSegmentStatusConfirmedForRepetitions() { return setSegmentStatusConfirmedForRepetitions; }
    public void setSetSegmentStatusConfirmedForRepetitions(boolean setSegmentStatusConfirmedForRepetitions) { this.setSegmentStatusConfirmedForRepetitions = setSegmentStatusConfirmedForRepetitions; }

    public boolean isLockSubsequentRepetitions() { return lockSubsequentRepetitions; }
    public void setLockSubsequentRepetitions(boolean lockSubsequentRepetitions) { this.lockSubsequentRepetitions = lockSubsequentRepetitions; }
}
