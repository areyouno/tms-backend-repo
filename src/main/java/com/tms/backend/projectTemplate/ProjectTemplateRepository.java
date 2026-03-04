package com.tms.backend.projectTemplate;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectTemplateRepository extends JpaRepository<ProjectTemplate, Long>{
    List<ProjectTemplate> findByUserId(Long userId);

    // find all non-delete templates
    @Query("SELECT t FROM ProjectTemplate t WHERE t.deleted = false")
    List<ProjectTemplate> findAllActive();

    //find all deleted
    @Query("SELECT t FROM ProjectTemplate t WHERE t.deleted = true")
    List<ProjectTemplate> findDeleted();

    @Query("SELECT t FROM ProjectTemplate t WHERE t.id = :id AND t.deleted = false")
    Optional<ProjectTemplate> findByIdAndNotDeleted(@Param("id") Long id);

    @Query("SELECT t FROM ProjectTemplate t WHERE t.id = :id")
    Optional<ProjectTemplate> findByIdIncludingDeleted(@Param("id") Long id);
}