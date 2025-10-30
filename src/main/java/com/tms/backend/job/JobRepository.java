package com.tms.backend.job;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long>{
    List<Job> findByProjectId(Long projectId);
    List<Job> findByProjectIdAndDeletedTrue(Long projectId);
    List<Job> findByJobOwnerUidAndDeletedTrue(String uid);
}
