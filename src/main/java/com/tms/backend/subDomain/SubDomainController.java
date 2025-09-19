package com.tms.backend.subDomain;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subdomains")
@Validated
public class SubDomainController {
    @Autowired
    private SubDomainService subDomainService;
    
    @GetMapping
    public List<SubDomain> getActiveClients() {
        return subDomainService.getActiveSubDomains();
    }

    @GetMapping("/ordered")
    public List<SubDomain> getActiveOrdered() {
        return subDomainService.getActiveSDOrdered();
    }
}
