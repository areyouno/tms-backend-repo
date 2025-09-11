package com.tms.backend.projectTemplate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectTemplateRepository extends JpaRepository<ProjectTemplate, Long>{
    
}
