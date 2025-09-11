package com.tms.backend.dto;

public class StatusDTO {
    private String value;  // enum name: "NEW", "EMAILED", etc.
    private String label;  // display name: "New", "Emailed to provider", etc.
    
    public StatusDTO(String value, String label) {
        this.value = value;
        this.label = label;
    }
    
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}