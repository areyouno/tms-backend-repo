package com.tms.backend.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tms.backend.dto.UserDTO;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUid(String uid);

    Optional<User> findByEmail(String email);

    UserDTO getUserById(Long id);

    List<User> findByRoleNameIn(List<String> roleNames);
}