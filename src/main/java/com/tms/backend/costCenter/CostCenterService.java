package com.tms.backend.costCenter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;

@Service
public class CostCenterService {
    @Autowired
    private CostCenterRepository repo;
    
    public List<CostCenter> getActiveCostCenters() {
        return repo.findByActiveTrue();
    }

    public List<CostCenter> getActiveCCOrdered() {
        return repo.findActiveCostCenterOrderByName();
    }
    
    public void softDeleteCostCenter(Long id) {
    CostCenter cc = repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("CostCenter not found"));
        cc.setActive(false);
        repo.save(cc);
    }
    
}
