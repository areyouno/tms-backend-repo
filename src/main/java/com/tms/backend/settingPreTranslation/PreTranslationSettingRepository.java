package com.tms.backend.settingPreTranslation;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tms.backend.user.User;

@Repository
public interface PreTranslationSettingRepository extends JpaRepository<PreTranslationSetting, Long> {
    Optional<PreTranslationSetting> findByUser(User user);

    boolean existsByUserIsNull();

    Optional<PreTranslationSetting> findFirstByUserIsNull();
}
