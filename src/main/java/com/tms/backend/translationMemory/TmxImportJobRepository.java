package com.tms.backend.translationMemory;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TmxImportJobRepository extends JpaRepository<TmxImportJob, String> {
    List<TmxImportJob> findByStatusIn(List<String> statuses);
    List<TmxImportJob> findByUserName(String userName);
}
