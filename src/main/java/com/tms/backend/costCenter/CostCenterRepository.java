package com.tms.backend.costCenter;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CostCenterRepository extends JpaRepository<CostCenter, Long>{
       List<CostCenter> findByActiveTrue(); // find all active

       List<CostCenter> findByActive(boolean active); // find active/inactive status

       @Query("SELECT cc FROM CostCenter cc WHERE cc.active = true ORDER BY cc.name")
       List<CostCenter> findActiveCostCenterOrderByName();
}
