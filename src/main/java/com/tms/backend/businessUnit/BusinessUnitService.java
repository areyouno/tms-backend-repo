package com.tms.backend.businessUnit;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;

@Service
public class BusinessUnitService {
    @Autowired
    private BusinessUnitRepository repo;
    
    public List<BusinessUnit> getActiveBusinessUnits() {
        return repo.findByActiveTrue();
    }

    public List<BusinessUnit> getActiveBUOrdered() {
        return repo.findActiveBusinessUnitsOrderByName();
    }
    
    public void softDeleteBusinessUnit(Long id) {
        BusinessUnit bu = repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BusinessUnit not found"));
        bu.setActive(false);
        repo.save(bu);
    }
}
