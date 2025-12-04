package com.tms.backend.vendor;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;

@Service
public class VendorService {
    @Autowired
    private VendorRepository repo;

    public Vendor createVendor(Vendor vendor) {
        return repo.save(vendor);
    }
    
    public List<Vendor> getActiveVendors() {
        return repo.findByActiveTrue();
    }

    public List<Vendor> getActiveVenOrdered() {
        return repo.findActiveVendorOrderByName();
    }
    
    public void softDeleteVendor(Long id) {
        Vendor v = repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Vendor not found"));
        v.setActive(false);
        repo.save(v);
    }
}
