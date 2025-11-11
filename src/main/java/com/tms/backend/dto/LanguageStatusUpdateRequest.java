package com.tms.backend.dto;

import java.util.List;

public class LanguageStatusUpdateRequest {
    private List<String> activate;
    private List<String> deactivate;

    public List<String> getActivate() {
        return activate;
    }
    public void setActivate(List<String> activate) {
        this.activate = activate;
    }
    public List<String> getDeactivate() {
        return deactivate;
    }
    public void setDeactivate(List<String> deactivate) {
        this.deactivate = deactivate;
    }
}
