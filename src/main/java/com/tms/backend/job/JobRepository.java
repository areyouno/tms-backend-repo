package com.tms.backend.job;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>{
    List<Job> findByProject_IdAndDeletedFalse(Long projectId);
    List<Job> findByProject_IdAndDeletedTrue(Long projectId);
    List<Job> findByJobOwnerUidAndDeletedTrue(String uid);

    List<Job> findByJobOwnerIdAndDeletedFalse(Long id);

    @Query("SELECT j FROM Job j WHERE j.deleted = false")
    List<Job> findAllActive(); // all active

    @Query("SELECT j FROM Job j WHERE j.deleted = true")
    List<Job> findAllInactive(); // recycle bin

    // For folder/filepath naming:
    // Ordinal position (1-based) of a source-file group within its project, ranked by
    // sourceGroupId (which is the id of that group's first job, so this is upload order).
    // Used to assign each uploaded file a stable "file1", "file2", ... storage folder name.
    @Query("SELECT COUNT(DISTINCT j.sourceGroupId) FROM Job j WHERE j.project.id = :projectId AND j.sourceGroupId <= :sourceGroupId")
    long countSourceFileGroupsUpTo(@Param("projectId") Long projectId, @Param("sourceGroupId") Long sourceGroupId);
}
