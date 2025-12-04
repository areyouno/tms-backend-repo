package com.tms.backend.vendor;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {
     List<Vendor> findByActiveTrue(); // find all active

    List<Vendor> findByActive(boolean active); // find active/inactive status

    @Query("SELECT v FROM Vendor v WHERE v.active = true ORDER BY v.name")
    List<Vendor> findActiveVendorOrderByName();
}
