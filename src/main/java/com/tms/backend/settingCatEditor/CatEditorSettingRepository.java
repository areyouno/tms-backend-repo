package com.tms.backend.settingCatEditor;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tms.backend.user.User;

@Repository
public interface CatEditorSettingRepository
        extends JpaRepository<CatEditorSetting, Long> {

    Optional<CatEditorSetting> findByUser(User user);

    boolean existsByUserIsNull();

    Optional<CatEditorSetting> findFirstByUserIsNull();
}
