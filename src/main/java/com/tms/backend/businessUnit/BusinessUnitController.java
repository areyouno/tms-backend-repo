package com.tms.backend.businessUnit;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/bu")
public class BusinessUnitController {
    @Autowired
    private BusinessUnitService businessUnitService;
    
    @GetMapping("/active")
    public List<BusinessUnit> getActiveBusinessUnits() {
        return businessUnitService.getActiveBusinessUnits();
    }

    @GetMapping("/active-ordered")
    public List<BusinessUnit> getActiveOrdered() {
        return businessUnitService.getActiveBUOrdered();
    }
    
}
