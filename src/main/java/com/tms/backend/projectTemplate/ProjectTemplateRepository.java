package com.tms.backend.projectTemplate;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectTemplateRepository extends JpaRepository<ProjectTemplate, Long>{
    List<ProjectTemplate> findByUserId(Long userId);
}
