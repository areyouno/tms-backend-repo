package com.tms.backend.workflowSteps;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {
    List<WorkflowStep> findAllByOrderByDisplayOrderAsc();
    boolean existsByName(String name);
    boolean existsByAbbreviation(String abbreviation);
    boolean existsByDisplayOrder(Integer displayOrder);
}
