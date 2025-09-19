package com.tms.backend.businessUnit;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/bu")
@Validated
public class BusinessUnitController {
    @Autowired
    private BusinessUnitService businessUnitService;
    
    @GetMapping
    public List<BusinessUnit> getActiveBusinessUnits() {
        return businessUnitService.getActiveBusinessUnits();
    }

    @GetMapping("/ordered")
    public List<BusinessUnit> getActiveOrdered() {
        return businessUnitService.getActiveBUOrdered();
    }
    
}
