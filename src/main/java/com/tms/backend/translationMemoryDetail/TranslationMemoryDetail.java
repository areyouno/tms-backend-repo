package com.tms.backend.translationMemoryDetail;

import java.time.LocalDateTime;

import com.tms.backend.translationMemory.TranslationMemory;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class TranslationMemoryDetail {
    @Id
    private Long detailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tm_id", nullable = false)
    private TranslationMemory translationMemory;
    
    private String sourceText;
    private String targetText;
    private String matchQuality;
    private LocalDateTime createDate;
    private LocalDateTime lastModifiedDate;

    public Long getDetailId() {
        return detailId;
    }
    public void setDetailId(Long detailId) {
        this.detailId = detailId;
    }
    public TranslationMemory getTranslationMemory() {
        return translationMemory;
    }
    public void setTranslationMemory(TranslationMemory translationMemory) {
        this.translationMemory = translationMemory;
    }
    public String getSourceText() {
        return sourceText;
    }
    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }
    public String getTargetText() {
        return targetText;
    }
    public void setTargetText(String targetText) {
        this.targetText = targetText;
    }
    public String getMatchQuality() {
        return matchQuality;
    }
    public void setMatchQuality(String matchQuality) {
        this.matchQuality = matchQuality;
    }
    public LocalDateTime getCreateDate() {
        return createDate;
    }
    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }
    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }
    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
