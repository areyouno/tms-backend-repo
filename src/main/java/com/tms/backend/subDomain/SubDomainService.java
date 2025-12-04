package com.tms.backend.subDomain;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SubDomainService {
    @Autowired
    private SubDomainRepository repo;

    public SubDomain createSubDomain(SubDomain subdomain) {
        return repo.save(subdomain);
    }
    
    public List<SubDomain> getActiveSubDomains() {
        return repo.findByActiveTrue();
    }

    public List<SubDomain> getActiveSDOrdered() {
        return repo.findActiveSubDomainOrderByName();
    }
    
    public void softDeleteSubDomain(Long id) {
        SubDomain sd = repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("SubDomain not found"));
        sd.setActive(false);
        repo.save(sd);
    }
}
