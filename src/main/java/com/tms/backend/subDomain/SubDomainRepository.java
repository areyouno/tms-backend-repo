package com.tms.backend.subDomain;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SubDomainRepository extends JpaRepository<SubDomain, Long>{
    List<SubDomain> findByActiveTrue(); // find all active

    List<SubDomain> findByActive(boolean active); // find active/inactive status

    @Query("SELECT sd FROM SubDomain sd WHERE sd.active = true ORDER BY sd.name")
    List<SubDomain> findActiveSubDomainOrderByName();
}
