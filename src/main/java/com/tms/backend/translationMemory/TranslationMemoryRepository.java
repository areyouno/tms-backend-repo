package com.tms.backend.translationMemory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TranslationMemoryRepository extends JpaRepository<TranslationMemory, Long>{
    
}
