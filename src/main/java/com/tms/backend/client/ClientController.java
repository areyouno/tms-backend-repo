package com.tms.backend.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.CreateClientRequest;
import com.tms.backend.dto.UpdateClientRequest;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/clients")
@Validated
public class ClientController {
    @Autowired
    private ClientService clientService;

    @PostMapping("/create")
    public ResponseEntity<Client> createClient(@Valid @RequestBody CreateClientRequest request) {
        Client created = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @GetMapping
    public List<Client> getActiveClients() {
        return clientService.getActiveClients();
    }

    @GetMapping("/ordered")
    public List<Client> getActiveOrdered() {
        return clientService.getActiveCLOrdered();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Client> getClientById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.getClientById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Client> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClientRequest request) {

        Client updated = clientService.updateClient(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDeleteClient(@PathVariable Long id) {
        clientService.softDeleteClient(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteClient(@PathVariable Long id) {
        clientService.hardDeleteClient(id);
        return ResponseEntity.noContent().build();
    }
}
