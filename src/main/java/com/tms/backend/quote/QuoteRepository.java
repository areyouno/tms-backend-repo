package com.tms.backend.quote;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuoteRepository extends JpaRepository<Quote, Long> {
    @Modifying
    @Query("UPDATE Quote q SET q.netRateScheme = null WHERE q.netRateScheme.id IN :ids")
    void clearNetRateSchemeByIds(@Param("ids") List<Long> ids);
}
