package com.tms.backend.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DomainRepository extends JpaRepository<Domain, Long>{
    List<Domain> findByActiveTrue(); // find all active

    List<Domain> findByActive(boolean active); // find active/inactive status

    @Query("SELECT d FROM Domain d WHERE d.active = true ORDER BY d.name")
    List<Domain> findActiveDomainOrderByName();
}
