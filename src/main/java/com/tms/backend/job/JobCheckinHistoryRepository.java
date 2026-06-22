package com.tms.backend.job;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobCheckinHistoryRepository extends JpaRepository<JobCheckinHistory, Long> {
    List<JobCheckinHistory> findByJobIdOrderByCheckedInAtDesc(Long jobId);
}
