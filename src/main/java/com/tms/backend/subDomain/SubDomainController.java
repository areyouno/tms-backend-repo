package com.tms.backend.subDomain;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/subdomain")
public class SubDomainController {
    @Autowired
    private SubDomainService subDomainService;
    
    @GetMapping("/active")
    public List<SubDomain> getActiveClients() {
        return subDomainService.getActiveSubDomains();
    }

    @GetMapping("/active-ordered")
    public List<SubDomain> getActiveOrdered() {
        return subDomainService.getActiveSDOrdered();
    }
}
