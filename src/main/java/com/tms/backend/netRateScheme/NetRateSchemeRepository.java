package com.tms.backend.netRateScheme;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NetRateSchemeRepository extends JpaRepository<NetRateScheme, Long>{
    @Modifying
    @Query("UPDATE NetRateScheme n SET n.isDefault = false WHERE n.isDefault = true")
    void clearCurrentDefault();

    boolean existsByIsDefaultTrue();

    Optional<NetRateScheme> findByIsDefaultTrue();
}
