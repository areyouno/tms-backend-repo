package com.tms.backend.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUid(String uid);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    List<User> findByRoleNameIn(List<String> roleNames);

    List<User> findByRoleNameInAndIsActiveTrue(List<String> roleNames);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    // Fetch only active users
    List<User> findByIsActiveTrue();
}