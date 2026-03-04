package com.tms.backend.settingCompletedFilesNaming;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tms.backend.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompletedFilesNamingSettingService {

    private final CompletedFilesNamingSettingRepository repository;

    @Transactional
    public void insertGlobalDefaultIfMissing() {
        if (repository.existsByUserIsNull()) {
            return;
        }

        CompletedFilesNamingSetting setting = new CompletedFilesNamingSetting();
        setting.setUser(null); // global default
        repository.save(setting);
    }

    @Transactional(readOnly = true)
    public CompletedFilesNamingSetting getForUser(User user) {
        return repository.findByUser(user)
                .orElseGet(() -> repository.findFirstByUserIsNull()
                        .orElseThrow(() -> new IllegalStateException("Global default missing")));
    }

    @Transactional
    public CompletedFilesNamingSetting updateForUser(User user, String folderName, boolean hasRule, String rule) {
        CompletedFilesNamingSetting setting = repository.findByUser(user)
                .orElseGet(() -> {
                    CompletedFilesNamingSetting s = new CompletedFilesNamingSetting();
                    s.setUser(user);
                    return s;
                });

        setting.setFolderName(folderName);
        setting.setHasNamingRule(hasRule);
        setting.setNamingRule(rule);

        return repository.save(setting);
    }
}
