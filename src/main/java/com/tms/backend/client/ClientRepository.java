package com.tms.backend.client;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface ClientRepository extends JpaRepository<Client, Long>{
    List<Client> findByActiveTrue(); // find all active

    List<Client> findByActive(boolean active); // find active/inactive status

    @Query("SELECT cl FROM Client cl WHERE cl.active = true ORDER BY cl.name")
    List<Client> findActiveClientOrderByName();
}
