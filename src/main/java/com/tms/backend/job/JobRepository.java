package com.tms.backend.job;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>{
    List<Job> findByProjectId(Long projectId);
    List<Job> findByProjectIdAndDeletedTrue(Long projectId);
    List<Job> findByJobOwnerUidAndDeletedTrue(String uid);

    List<Job> findByJobOwnerIdAndDeletedFalse(Long id);

    @Query("SELECT j FROM Job j WHERE j.deleted = false")
    List<Job> findAllActive(); // all active

    @Query("SELECT j FROM Job j WHERE j.deleted = true")
    List<Job> findAllInactive(); // recycle bin
}
