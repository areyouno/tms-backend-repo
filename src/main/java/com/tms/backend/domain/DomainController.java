package com.tms.backend.domain;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/domains")
@Validated
public class DomainController {
    @Autowired
    private DomainService domainService;
    
    @GetMapping
    public List<Domain> getActiveClients() {
        return domainService.getActiveDomains();
    }

    @GetMapping("/ordered")
    public List<Domain> getActiveOrdered() {
        return domainService.getActiveDomOrdered();
    }
}
