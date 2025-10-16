package com.tms.backend.analytics;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tms.backend.job.Job;

@Repository
public interface JobDeliveryAnalyticsRepository extends JpaRepository<Job, Long> {
  // start date count
   @Query(value = """
        SELECT
          DATE_FORMAT(j.due_date, '%Y') AS filterValue,
          SUM(CASE WHEN j.completed_date IS NOT NULL AND j.completed_date <= j.due_date THEN 1
                  WHEN j.completed_date IS NULL AND j.due_date > :endDate THEN 1 ELSE 0 END) AS onTime,
          SUM(CASE WHEN j.completed_date IS NOT NULL AND j.completed_date > j.due_date THEN 1
                  WHEN j.completed_date IS NULL AND j.due_date < :endDate THEN 1 ELSE 0 END) AS overdue
        FROM jobs j
        WHERE j.due_date BETWEEN :startDate AND :endDate
          AND (:providerId IS NULL OR j.provider_id = :providerId)
          AND (:localePair IS NULL OR j.source_lang = :localePair)
          AND (:status IS NULL OR j.status = :status)
          AND (:projectId IS NULL OR j.project_id = :projectId)
        GROUP BY DATE_FORMAT(j.due_date, '%Y')
        ORDER BY filterValue
        """, nativeQuery = true)
    List<JobDeliveryStatistics> countByYear(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("providerId") Long providerId,
        @Param("localePair") String localePair,
        @Param("status") String status,
        @Param("projectId") Long projectId);

    @Query(value = """
        SELECT
          DATE_FORMAT(j.due_date, '%Y-%m') AS filterValue,
          SUM(CASE WHEN j.completed_date IS NOT NULL AND j.completed_date <= j.due_date THEN 1
                  WHEN j.completed_date IS NULL AND j.due_date > :endDate THEN 1 ELSE 0 END) AS onTime,
          SUM(CASE WHEN j.completed_date IS NOT NULL AND j.completed_date > j.due_date THEN 1
                  WHEN j.completed_date IS NULL AND j.due_date < :endDate THEN 1 ELSE 0 END) AS overdue
        FROM jobs j
        WHERE j.due_date BETWEEN :startDate AND :endDate
          AND (:providerId IS NULL OR j.provider_id = :providerId)
          AND (:localePair IS NULL OR j.source_lang = :localePair)
          AND (:status IS NULL OR j.status = :status)
          AND (:projectId IS NULL OR j.project_id = :projectId)
        GROUP BY DATE_FORMAT(j.due_date, '%Y-%m')
        ORDER BY filterValue
        """, nativeQuery = true)
    List<JobDeliveryStatistics> countByMonth(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("providerId") Long providerId,
        @Param("localePair") String localePair,
        @Param("status") String status,
        @Param("projectId") Long projectId);

    @Query(value = """
        SELECT
          YEARWEEK(j.due_date, 1) AS filterValue,
          SUM(CASE WHEN j.completed_date IS NOT NULL AND j.completed_date <= j.due_date THEN 1
                  WHEN j.completed_date IS NULL AND j.due_date > :endDate THEN 1 ELSE 0 END) AS onTime,
          SUM(CASE WHEN j.completed_date IS NOT NULL AND j.completed_date > j.due_date THEN 1
                  WHEN j.completed_date IS NULL AND j.due_date < :endDate THEN 1 ELSE 0 END) AS overdue
        FROM jobs j
        WHERE j.due_date BETWEEN :startDate AND :endDate
          AND (:providerId IS NULL OR j.provider_id = :providerId)
          AND (:localePair IS NULL OR j.source_lang = :localePair)
          AND (:status IS NULL OR j.status = :status)
          AND (:projectId IS NULL OR j.project_id = :projectId)
        GROUP BY YEARWEEK(j.due_date, 1)
        ORDER BY filterValue
        """, nativeQuery = true)
    List<JobDeliveryStatistics> countByWeek(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("providerId") Long providerId,
        @Param("localePair") String localePair,
        @Param("status") String status,
        @Param("projectId") Long projectId);

    @Query(value = """
        SELECT
          DATE_FORMAT(j.due_date, '%Y-%m-%d') AS filterValue,
          SUM(CASE WHEN j.completed_date IS NOT NULL AND j.completed_date <= j.due_date THEN 1
                  WHEN j.completed_date IS NULL AND j.due_date > :endDate THEN 1 ELSE 0 END) AS onTime,
          SUM(CASE WHEN j.completed_date IS NOT NULL AND j.completed_date > j.due_date THEN 1
                  WHEN j.completed_date IS NULL AND j.due_date < :endDate THEN 1 ELSE 0 END) AS overdue
        FROM jobs j
        WHERE j.due_date BETWEEN :startDate AND :endDate
          AND (:providerId IS NULL OR j.provider_id = :providerId)
          AND (:localePair IS NULL OR j.source_lang = :localePair)
          AND (:status IS NULL OR j.status = :status)
          AND (:projectId IS NULL OR j.project_id = :projectId)
        GROUP BY DATE_FORMAT(j.due_date, '%Y-%m-%d')
        ORDER BY filterValue
        """, nativeQuery = true)
    List<JobDeliveryStatistics> countByDay(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("providerId") Long providerId,
        @Param("localePair") String localePair,
        @Param("status") String status,
        @Param("projectId") Long projectId);
        // end date count

    @Query(value = """
        SELECT
          DATE_FORMAT(j.due_date, '%Y-%m-%d %H:00:00') AS filterValue,
          SUM(CASE WHEN j.completed_date IS NOT NULL AND j.completed_date <= j.due_date THEN 1
                   WHEN j.completed_date IS NULL AND j.due_date > :endDate THEN 1 ELSE 0 END) AS onTime,
          SUM(CASE WHEN j.completed_date IS NOT NULL AND j.completed_date > j.due_date THEN 1
                   WHEN j.completed_date IS NULL AND j.due_date < :endDate THEN 1 ELSE 0 END) AS overdue
        FROM jobs j
        WHERE j.due_date BETWEEN :startDate AND :endDate
          AND (:providerId IS NULL OR j.provider_id = :providerId)
          AND (:localePair IS NULL OR j.source_lang = :localePair)
          AND (:status IS NULL OR j.status = :status)
          AND (:projectId IS NULL OR j.project_id = :projectId)
        GROUP BY DATE_FORMAT(j.due_date, '%Y-%m-%d %H:00:00')
        ORDER BY filterValue
        """, nativeQuery = true)
    List<JobDeliveryStatistics> countByHour(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("providerId") Long providerId,
        @Param("localePair") String localePair,
        @Param("status") String status,
        @Param("projectId") Long projectId);

        // Count by MINUTE
    @Query(value = """
        SELECT
          DATE_FORMAT(j.due_date, '%Y-%m-%d %H:%i:00') AS filterValue,
          SUM(CASE WHEN j.completed_date IS NOT NULL AND j.completed_date <= j.due_date THEN 1
                   WHEN j.completed_date IS NULL AND j.due_date > :endDate THEN 1 ELSE 0 END) AS onTime,
          SUM(CASE WHEN j.completed_date IS NOT NULL AND j.completed_date > j.due_date THEN 1
                   WHEN j.completed_date IS NULL AND j.due_date < :endDate THEN 1 ELSE 0 END) AS overdue
        FROM jobs j
        WHERE j.due_date BETWEEN :startDate AND :endDate
          AND (:providerId IS NULL OR j.provider_id = :providerId)
          AND (:localePair IS NULL OR j.source_lang = :localePair)
          AND (:status IS NULL OR j.status = :status)
          AND (:projectId IS NULL OR j.project_id = :projectId)
        GROUP BY DATE_FORMAT(j.due_date, '%Y-%m-%d %H:%i:00')
        ORDER BY filterValue
        """, nativeQuery = true)
    List<JobDeliveryStatistics> countByMinute(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("providerId") Long providerId,
        @Param("localePair") String localePair,
        @Param("status") String status,
        @Param("projectId") Long projectId);

    // start provider count
    @Query(value = """
      SELECT
        CONCAT(u.last_name, ', ', u.first_name) AS filterValue,
        SUM(CASE WHEN j.completed_date IS NOT NULL AND j.completed_date <= j.due_date THEN 1
                WHEN j.completed_date IS NULL AND j.due_date > :endDate THEN 1 ELSE 0 END) AS onTime,
        SUM(CASE WHEN j.completed_date IS NOT NULL AND j.completed_date > j.due_date THEN 1
                WHEN j.completed_date IS NULL AND j.due_date < :endDate THEN 1 ELSE 0 END) AS overdue
      FROM jobs j
      JOIN users u ON j.provider_id = u.user_id
      WHERE j.due_date BETWEEN :startDate AND :endDate
        AND (:providerId IS NULL OR j.provider_id = :providerId)
        AND (:localePair IS NULL OR j.source_lang = :localePair)
        AND (:status IS NULL OR j.status = :status)
        AND (:projectId IS NULL OR j.project_id = :projectId)
      GROUP BY u.last_name, u.first_name
      ORDER BY filterValue
      """, nativeQuery = true)
  List<JobDeliveryStatistics> countByProvider(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("providerId") Long providerId,
      @Param("localePair") String localePair,
      @Param("status") String status,
      @Param("projectId") Long projectId);
      // end provider count

    // start locale count
   @Query(value = """
      SELECT
        j.source_lang AS filterValue,
        SUM(CASE WHEN j.completed_date IS NOT NULL AND j.completed_date <= j.due_date THEN 1
                WHEN j.completed_date IS NULL AND j.due_date > :endDate THEN 1 ELSE 0 END) AS onTime,
        SUM(CASE WHEN j.completed_date IS NOT NULL AND j.completed_date > j.due_date THEN 1
                WHEN j.completed_date IS NULL AND j.due_date < :endDate THEN 1 ELSE 0 END) AS overdue
      FROM jobs j
      WHERE j.due_date BETWEEN :startDate AND :endDate
        AND (:providerId IS NULL OR j.provider_id = :providerId)
        AND (:localePair IS NULL OR j.source_lang = :localePair)
        AND (:status IS NULL OR j.status = :status)
        AND (:projectId IS NULL OR j.project_id = :projectId)
      GROUP BY j.source_lang
      ORDER BY filterValue
      """, nativeQuery = true)
  List<JobDeliveryStatistics> countByLocale(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("providerId") Long providerId,
      @Param("localePair") String localePair,
      @Param("status") String status,
      @Param("projectId") Long projectId);
      // end locale count

      // start project count
  @Query(value = """
      SELECT
        p.name AS filterValue,
        SUM(CASE WHEN j.completed_date IS NOT NULL AND j.completed_date <= j.due_date THEN 1
                WHEN j.completed_date IS NULL AND j.due_date > :endDate THEN 1 ELSE 0 END) AS onTime,
        SUM(CASE WHEN j.completed_date IS NOT NULL AND j.completed_date > j.due_date THEN 1
                WHEN j.completed_date IS NULL AND j.due_date < :endDate THEN 1 ELSE 0 END) AS overdue
      FROM jobs j
      JOIN projects p ON j.project_id = p.id
      WHERE j.due_date BETWEEN :startDate AND :endDate
        AND (:providerId IS NULL OR j.provider_id = :providerId)
        AND (:localePair IS NULL OR j.source_lang = :localePair)
        AND (:status IS NULL OR j.status = :status)
        AND (:projectId IS NULL OR j.project_id = :projectId)
      GROUP BY p.name
      ORDER BY filterValue
      """, nativeQuery = true)
  List<JobDeliveryStatistics> countByProject(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("providerId") Long providerId,
      @Param("localePair") String localePair,
      @Param("status") String status,
      @Param("projectId") Long projectId);
      // end project count

    // start status count
  @Query(value = """
      SELECT
        j.status AS filterValue,
        SUM(CASE WHEN j.completed_date IS NOT NULL AND j.completed_date <= j.due_date THEN 1
                WHEN j.completed_date IS NULL AND j.due_date > :endDate THEN 1 ELSE 0 END) AS onTime,
        SUM(CASE WHEN j.completed_date IS NOT NULL AND j.completed_date > j.due_date THEN 1
                WHEN j.completed_date IS NULL AND j.due_date < :endDate THEN 1 ELSE 0 END) AS overdue
      FROM jobs j
      WHERE j.due_date BETWEEN :startDate AND :endDate
        AND (:providerId IS NULL OR j.provider_id = :providerId)
        AND (:localePair IS NULL OR j.source_lang = :localePair)
        AND (:status IS NULL OR j.status = :status)
        AND (:projectId IS NULL OR j.project_id = :projectId)
      GROUP BY j.status
      ORDER BY filterValue
      """, nativeQuery = true)
  List<JobDeliveryStatistics> countByStatus(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("providerId") Long providerId,
      @Param("localePair") String localePair,
      @Param("status") String status,
      @Param("projectId") Long projectId);
    // end status count

}