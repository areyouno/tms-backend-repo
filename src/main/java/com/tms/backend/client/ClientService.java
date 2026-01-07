package com.tms.backend.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tms.backend.dto.CreateClientRequest;
import com.tms.backend.dto.UpdateClientRequest;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ClientService {
    @Autowired
    private ClientRepository repo;

    public Client createClient(CreateClientRequest req) {
        Client client = new Client();
        client.setName(req.name());
        client.setExternalId(req.externalId());
        client.setActive(true);

        return repo.save(client);
    }
    
    public List<Client> getActiveClients() {
        return repo.findByActiveTrue();
    }

    public List<Client> getActiveCLOrdered() {
        return repo.findActiveClientOrderByName();
    }

    public Client updateClient(Long id, UpdateClientRequest req) {
        Client client = repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Client not found"));

        client.setName(req.name());
        client.setExternalId(req.externalId());

        return repo.save(client);
    }
    
    public void softDeleteClient(Long id) {
        Client cl = repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Client not found"));
        cl.setActive(false);
        repo.save(cl);
    }

    public void hardDeleteClient(Long id) {
        if (!repo.existsById(id)) {
            throw new EntityNotFoundException("Client not found");
        }
        repo.deleteById(id);
    }
}
