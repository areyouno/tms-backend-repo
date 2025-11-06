package com.tms.backend.project;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>{
    @Query("SELECT p FROM Project p WHERE p.owner.id = :id AND p.deleted = false")
    List<Project> findByOwnerId(Long id);

    // Find all non-deleted projects
    @Query("SELECT p FROM Project p WHERE p.deleted = false")
    List<Project> findAllActive();
    
    // Find by ID (non-deleted only)
    @Query("SELECT p FROM Project p WHERE p.id = :id AND p.deleted = false")
    Optional<Project> findByIdAndNotDeleted(@Param("id") Long id);

    @Query("SELECT p FROM Project p WHERE p.owner.email = :email AND p.deleted = false")
    List<Project> findByOwnerEmail(@Param("email") String email);

    @Query("""
    SELECT DISTINCT p 
    FROM Project p 
    LEFT JOIN FETCH p.jobs j 
    LEFT JOIN FETCH j.workflowSteps 
    WHERE p.id = :id
            """)
    Optional<Project> findByIdWithJobsAndSteps(@Param("id") Long id);

    @Query("SELECT p FROM Project p WHERE p.deleted = true")
    List<Project> findDeleted();

    @Query("SELECT p FROM Project p WHERE p.id = :id")
    Optional<Project> findByIdIncludingDeleted(@Param("id") Long id);

    @Query("SELECT p FROM Project p WHERE p.deleted = true AND p.owner.uid = :uid")
    List<Project> findSoftDeletedByOwner(@Param("uid") String uid);

    // delete permanently
    @Modifying
    @Query("DELETE FROM Project p WHERE p.id = :id")
    void hardDeleteById(@Param("id") Long id);

    // find deleted files for clean up
    @Query("SELECT p FROM Project p WHERE p.deleted = true AND p.deletedDate < :cutoffDate")
    List<Project> findSoftDeletedBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}
