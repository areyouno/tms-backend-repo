package com.tms.backend.settingCompletedFilesNaming;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tms.backend.user.User;

@Repository
public interface CompletedFilesNamingSettingRepository extends JpaRepository<CompletedFilesNamingSetting, Long> {

    Optional<CompletedFilesNamingSetting> findByUser(User user);

    boolean existsByUserIsNull();

    Optional<CompletedFilesNamingSetting> findFirstByUserIsNull();
}
