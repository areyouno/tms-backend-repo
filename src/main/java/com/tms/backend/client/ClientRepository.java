package com.tms.backend.client;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tms.backend.netRateScheme.NetRateScheme;


@Repository
public interface ClientRepository extends JpaRepository<Client, Long>{
    List<Client> findByActiveTrue();

    List<Client> findByActive(boolean active);

    @Query("SELECT cl FROM Client cl WHERE cl.active = true ORDER BY cl.name")
    List<Client> findActiveClientOrderByName();

    Optional<Client> findByNetRateScheme(NetRateScheme netRateScheme);

    @Modifying
    @Query("UPDATE Client cl SET cl.netRateScheme = null WHERE cl.netRateScheme.id IN :ids")
    void clearNetRateSchemeByIds(@Param("ids") List<Long> ids);
}
