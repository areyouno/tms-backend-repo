package com.tms.backend.costCenter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/cc")
public class CostCenterController {
    @Autowired
    private CostCenterService costCenterService;
    
    @GetMapping
    public List<CostCenter> getActiveCostCenters() {
        return costCenterService.getActiveCostCenters();
    }

    @GetMapping("/ordered")
    public List<CostCenter> getActiveOrdered() {
        return costCenterService.getActiveCCOrdered();
    }
}
