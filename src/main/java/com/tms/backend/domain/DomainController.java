package com.tms.backend.domain;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/domain")
public class DomainController {
    @Autowired
    private DomainService domainService;
    
    @GetMapping("/active")
    public List<Domain> getActiveClients() {
        return domainService.getActiveDomains();
    }

    @GetMapping("/active-ordered")
    public List<Domain> getActiveOrdered() {
        return domainService.getActiveDomOrdered();
    }
}
