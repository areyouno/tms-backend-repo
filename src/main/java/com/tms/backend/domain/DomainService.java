package com.tms.backend.domain;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;

@Service
public class DomainService {
    @Autowired
    private DomainRepository repo;

    public Domain createDomain(Domain domain) {
        return repo.save(domain);
    }
    
    public List<Domain> getActiveDomains() {
        return repo.findByActiveTrue();
    }

    public List<Domain> getActiveDomOrdered() {
        return repo.findActiveDomainOrderByName();
    }
    
    public void softDeleteDomain(Long id) {
        Domain d = repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Domain not found"));
        d.setActive(false);
        repo.save(d);
    }
}
