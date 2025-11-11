package com.tms.backend.language;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "languages")
public class Language {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rfc_code", nullable = false, unique = true)
    private String rfcCode;

    @Column(name = "language_name", nullable = false)
    private String languageName;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    // Constructors
    public Language() {}

    public Language(String rfcCode, String languageName, boolean isActive) {
        this.rfcCode = rfcCode;
        this.languageName = languageName;
        this.isActive = isActive;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public String getRfcCode() {
        return rfcCode;
    }

    public void setRfcCode(String rfcCode) {
        this.rfcCode = rfcCode;
    }

    public String getLanguageName() {
        return languageName;
    }

    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
