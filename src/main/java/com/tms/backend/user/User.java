package com.tms.backend.user;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tms.backend.project.Project;
import com.tms.backend.role.Role;
import com.tms.backend.verificationToken.VerificationToken;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;
    
    @Column(unique = true, nullable = false, updatable = false)
    private String uid;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = true)
    private String password;

    @Column(nullable = true)
    private String firstName;

    private String lastName;

    private String country;

    private boolean isVerified = false;
    private boolean isProfileComplete = false;

    private String referralSource;

    private String organizationName;
    private String organizationSize;

    private String username;
    private boolean isActive;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = true)
    private Role role;

    private boolean agreedToTerms = false;

    @JsonIgnore
    @OneToMany(mappedBy = "owner")
    private Set<Project> ownedProjects = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<VerificationToken> verificationTokens;

    private ZoneId timeZone = ZoneId.of("Asia/Manila"); // set default value

    @Column(columnDefinition = "VARCHAR(10)")
    private String sourceLang;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_target_languages", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "language_code", length = 10)
    private Set<String> targetLanguages = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    private String note;

    @PrePersist
    private void generateUid() {
        if (uid == null) {
            uid = UUID.randomUUID().toString();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Boolean getAgreedToTerms() {
        return agreedToTerms;
    }

    public void setAgreedToTerms(Boolean agreedToTerms) {
        this.agreedToTerms = agreedToTerms;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }
    
    public String getOrganizationSize() {
        return organizationSize;
    }

    public void setOrganizationSize(String organizationSize) {
        this.organizationSize = organizationSize;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

     public String getReferralSource() {
        return referralSource;
    }

    public void setReferralSource(String referralSource) {
        this.referralSource = referralSource;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean isVerified) {
        this.isVerified = isVerified;
    }

    public boolean isProfileComplete() {
        return isProfileComplete;
    }

    public void setProfileComplete(boolean isProfileComplete) {
        this.isProfileComplete = isProfileComplete;
    }

    public Set<Project> getOwnedProjects() {
        return ownedProjects;
    }

    public void setOwnedProjects(Set<Project> ownedProjects) {
        this.ownedProjects = ownedProjects;
    }

    public Set<VerificationToken> getVerificationTokens() {
        return verificationTokens;
    }
    public void setVerificationTokens(Set<VerificationToken> verificationTokens) {
        this.verificationTokens = verificationTokens;
    }

    public void addVerificationToken(VerificationToken token) {
        verificationTokens.add(token);
        token.setUser(this); // keep both table in sync
    }

    public void removeVerificationToken(VerificationToken token) {
        verificationTokens.remove(token);
        token.setUser(null);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public ZoneId getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(ZoneId timeZone) {
        this.timeZone = timeZone;
    }

    public String getSourceLang() {
        return sourceLang;
    }

    public void setSourceLang(String sourceLang) {
        this.sourceLang = sourceLang;
    }

    public Set<String> getTargetLanguages() {
        return targetLanguages;
    }

    public void setTargetLanguages(Set<String> targetLanguages) {
        this.targetLanguages = targetLanguages;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
