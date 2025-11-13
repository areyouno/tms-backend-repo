package com.tms.backend.setting;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AutomationSettingRepository extends JpaRepository<AutomationSetting, Long> {
    Optional<AutomationSetting> findByUserUid(String uid);
}
