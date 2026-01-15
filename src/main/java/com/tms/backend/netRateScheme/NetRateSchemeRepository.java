package com.tms.backend.netRateScheme;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NetRateSchemeRepository extends JpaRepository<NetRateScheme, Long>{
    
}
