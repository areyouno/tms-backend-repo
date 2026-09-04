package com.tms.backend.taskList;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskListRepository extends JpaRepository<TaskList, Long> {
    List<TaskList> findByAssignee_UidOrderByCreateDateDesc(String assigneeUid);

    boolean existsByAssigneeId(Long assigneeId);

    List<TaskList> findByJobs_IdAndWorkflowStep_IdOrderByCreateDateDesc(Long jobId, Long workflowStepId);
}
