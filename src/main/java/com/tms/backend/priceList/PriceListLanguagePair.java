package com.tms.backend.priceList;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "price_list_language_pair")
public class PriceListLanguagePair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_list_id", nullable = false)
    @JsonIgnore
    private PriceList priceList;

    private String sourceLanguage;

    private String targetLanguage;

    private Double price = 0.0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PriceList getPriceList() { return priceList; }
    public void setPriceList(PriceList priceList) { this.priceList = priceList; }

    public String getSourceLanguage() { return sourceLanguage; }
    public void setSourceLanguage(String sourceLanguage) { this.sourceLanguage = sourceLanguage; }

    public String getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
