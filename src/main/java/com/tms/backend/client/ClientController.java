package com.tms.backend.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/client")
public class ClientController {
    @Autowired
    private ClientService clientService;
    
    @GetMapping("/active")
    public List<Client> getActiveClients() {
        return clientService.getActiveClients();
    }

    @GetMapping("/active-ordered")
    public List<Client> getActiveOrdered() {
        return clientService.getActiveCLOrdered();
    }
}
