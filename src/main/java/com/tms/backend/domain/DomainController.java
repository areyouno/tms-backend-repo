package com.tms.backend.domain;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/domains")
@Validated
public class DomainController {
    @Autowired
    private DomainService domainService;

    @PostMapping
    public ResponseEntity<Domain> createDomain(@RequestBody Domain domain) {
        Domain createdDomain = domainService.createDomain(domain);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDomain);
    }
    
    @GetMapping
    public List<Domain> getActiveDomains() {
        return domainService.getActiveDomains();
    }

    @GetMapping("/ordered")
    public List<Domain> getActiveOrdered() {
        return domainService.getActiveDomOrdered();
    }
}
