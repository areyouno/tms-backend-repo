package com.tms.backend.client;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ClientService {
    @Autowired
    private ClientRepository repo;
    
    public List<Client> getActiveClients() {
        return repo.findByActiveTrue();
    }

    public List<Client> getActiveCLOrdered() {
        return repo.findActiveClientOrderByName();
    }
    
    public void softDeleteClient(Long id) {
        Client cl = repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Client not found"));
        cl.setActive(false);
        repo.save(cl);
    }
}
