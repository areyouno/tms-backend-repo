package com.tms.backend.taskList;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskListRepository extends JpaRepository<TaskList, Long> {
    List<TaskList> findByAssignee_UidOrderByCreateDateDesc(String assigneeUid);

    @Query("SELECT DISTINCT tl FROM TaskList tl JOIN tl.jobs j "
        + "WHERE (:projectId IS NULL OR j.project.id = :projectId) "
        + "AND (:targetLangCode IS NULL OR tl.targetLang.rfcCode = :targetLangCode) "
        + "AND (:workflowStepId IS NULL OR tl.workflowStep.id = :workflowStepId) "
        + "ORDER BY tl.createDate DESC")
    List<TaskList> findByFilters(
        @Param("projectId") Long projectId,
        @Param("targetLangCode") String targetLangCode,
        @Param("workflowStepId") Long workflowStepId);
}
