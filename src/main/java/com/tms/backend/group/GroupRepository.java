package com.tms.backend.group;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupRepository extends JpaRepository<Group, Long> {

    boolean existsByName(String name);

    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.teamLeader LEFT JOIN FETCH g.teamMembers LEFT JOIN FETCH g.teamProjects WHERE g.id = :id")
    Optional<Group> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT g FROM Group g LEFT JOIN FETCH g.teamLeader LEFT JOIN FETCH g.teamMembers LEFT JOIN FETCH g.teamProjects")
    List<Group> findAllWithDetails();

    @Query("SELECT DISTINCT g FROM Group g LEFT JOIN FETCH g.teamLeader LEFT JOIN FETCH g.teamMembers LEFT JOIN FETCH g.teamProjects WHERE g.isGroupActive = true")
    List<Group> findAllActiveWithDetails();
}
