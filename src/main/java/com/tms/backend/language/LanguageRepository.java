package com.tms.backend.language;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LanguageRepository extends JpaRepository<Language, Long> {
    Optional<Language> findByRfcCode(String rfcCode);

    List<Language> findAllByRfcCodeIn(List<String> rfcCodes);

    List<Language> findByIsActiveTrue();

    List<Language> findByIsActiveFalse();

    @Query("SELECT l.rfcCode FROM Language l")
    List<String> findAllRfcCodes();
}
