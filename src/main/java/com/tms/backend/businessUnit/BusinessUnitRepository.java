package com.tms.backend.businessUnit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface BusinessUnitRepository extends JpaRepository<BusinessUnit, Long>{
    List<BusinessUnit> findByActiveTrue(); // find all active

    List<BusinessUnit> findByActive(boolean active); // find active/inactive status

    @Query("SELECT bu FROM BusinessUnit bu WHERE bu.active = true ORDER BY bu.name")
    List<BusinessUnit> findActiveBusinessUnitsOrderByName();

}
