package com.tms.backend.job;


import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tms.backend.dto.JobAnalyticsCountDTO;

public interface JobRepository extends JpaRepository<Job, Long>{
      @Query("""
            SELECT new com.tms.backend.dto.JobAnalyticsCountDTO(
                  SUM(CASE
                        WHEN j.completedDate IS NOT NULL AND j.completedDate <= j.dueDate
                        THEN 1L
                        WHEN j.completedDate IS NULL AND j.dueDate > :toDate
                        THEN 1L
                        ELSE 0L END),
                  SUM(CASE
                        WHEN j.completedDate IS NOT NULL AND j.completedDate > j.dueDate
                        THEN 1L
                        WHEN j.completedDate IS NULL AND j.dueDate < :toDate
                        THEN 1L
                        ELSE 0L END)
                      )
            FROM Job j
            WHERE j.dueDate IS NOT NULL
            AND (
            (j.completedDate BETWEEN :fromDate AND :toDate)
            OR (j.completedDate IS NULL AND j.dueDate BETWEEN :fromDate AND :toDate)
            )
      """)
    JobAnalyticsCountDTO getDeliveryByMonthCount(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);
}
