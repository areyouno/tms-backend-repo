package com.tms.backend.priceList;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.tms.backend.currency.Currency;
import com.tms.backend.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "price_list")
public class PriceList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "currency_id")
    private Currency currency;

    @Enumerated(EnumType.STRING)
    private BillingUnit billingUnit;

    @Column(name = "create_date", updatable = false)
    @CreationTimestamp
    private LocalDateTime createDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(
        mappedBy = "priceList",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    private List<PriceListLanguagePair> languagePairs = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }

    public BillingUnit getBillingUnit() { return billingUnit; }
    public void setBillingUnit(BillingUnit billingUnit) { this.billingUnit = billingUnit; }

    public LocalDateTime getCreateDate() { return createDate; }
    public void setCreateDate(LocalDateTime createDate) { this.createDate = createDate; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public List<PriceListLanguagePair> getLanguagePairs() { return languagePairs; }
    public void setLanguagePairs(List<PriceListLanguagePair> languagePairs) { this.languagePairs = languagePairs; }
}
