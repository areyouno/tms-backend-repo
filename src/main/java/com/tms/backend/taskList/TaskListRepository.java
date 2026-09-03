package com.tms.backend.taskList;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskListRepository extends JpaRepository<TaskList, Long> {
    List<TaskList> findByAssignee_UidOrderByCreateDateDesc(String assigneeUid);

    boolean existsByAssigneeId(Long assigneeId);

    List<TaskList> findByJobs_IdAndWorkflowStep_IdOrderByCreateDateDesc(Long jobId, Long workflowStepId);

    @Query("select distinct t from TaskList t join t.jobs j "
        + "where j.project.id = :projectId and t.tmProvisioningStatus = 'SKIPPED_NO_TEMPLATE_TM'")
    List<TaskList> findPendingTmProvisioningForProject(@Param("projectId") Long projectId);
}
