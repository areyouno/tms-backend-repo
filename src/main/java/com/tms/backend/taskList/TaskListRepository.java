package com.tms.backend.taskList;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskListRepository extends JpaRepository<TaskList, Long> {
    List<TaskList> findDistinctByJobs_Project_IdOrderByCreateDateDesc(Long projectId);

    List<TaskList> findAllByOrderByCreateDateDesc();

    List<TaskList> findByAssignee_UidOrderByCreateDateDesc(String assigneeUid);
}
