package com.tms.backend.translationMemory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tms.backend.translationMemoryDetail.TranslationMemoryDetail;
import com.tms.backend.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class TranslationMemory {
    @Id
    private Long tmId;
    private String name;
    private String sourceLang;
    private String targetLang;
    private LocalDateTime createDate;
    private String createdBy;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private User owner;
    private LocalDateTime updateDate;
    private Long segmentsCount;

    @OneToMany(mappedBy = "translationMemory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TranslationMemoryDetail> details = new ArrayList<>();
    
    public Long getTmId() {
        return tmId;
    }
    public void setTmId(Long tmId) {
        this.tmId = tmId;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getSourceLang() {
        return sourceLang;
    }
    public void setSourceLang(String sourceLang) {
        this.sourceLang = sourceLang;
    }
    public String getTargetLang() {
        return targetLang;
    }
    public void setTargetLang(String targetLang) {
        this.targetLang = targetLang;
    }
    public LocalDateTime getCreateDate() {
        return createDate;
    }
    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }
    public String getCreatedBy() {
        return createdBy;
    }
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    public User getOwner() {
        return owner;
    }
    public void setOwner(User owner) {
        this.owner = owner;
    }
    public LocalDateTime getUpdateDate() {
        return updateDate;
    }
    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }
    public Long getSegments() {
        return segmentsCount;
    }
    public void setSegments(Long segmentsCount) {
        this.segmentsCount = segmentsCount;
    }

    public List<TranslationMemoryDetail> getDetails() {
        return details;
    }

    public void setDetails(List<TranslationMemoryDetail> details) {
        this.details = details;
    }
    
    public void addDetail(TranslationMemoryDetail detail) {
        details.add(detail);
        detail.setTranslationMemory(this); // keep both sides in sync
    }

    public void removeDetail(TranslationMemoryDetail detail) {
        details.remove(detail);
        detail.setTranslationMemory(null);
    }
}
