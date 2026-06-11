package com.tms.backend.termbase;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TermbaseImportJobRepository extends JpaRepository<TermbaseImportJob, String> {
    List<TermbaseImportJob> findByStatusIn(List<String> statuses);
}
