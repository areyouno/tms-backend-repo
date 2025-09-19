package com.tms.backend.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/clients")
@Validated
public class ClientController {
    @Autowired
    private ClientService clientService;
    
    @GetMapping
    public List<Client> getActiveClients() {
        return clientService.getActiveClients();
    }

    @GetMapping("/ordered")
    public List<Client> getActiveOrdered() {
        return clientService.getActiveCLOrdered();
    }
}
