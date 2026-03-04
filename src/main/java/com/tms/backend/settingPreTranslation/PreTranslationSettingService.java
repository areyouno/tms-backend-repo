package com.tms.backend.settingPreTranslation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tms.backend.dto.PreTranslationSettingDTO;
import com.tms.backend.user.User;



@Service
public class PreTranslationSettingService {

    private final PreTranslationSettingRepository preTranslationSettingRepository;

    public PreTranslationSettingService(PreTranslationSettingRepository preTranslationSettingRepository) {
        this.preTranslationSettingRepository = preTranslationSettingRepository;
    }

    @Transactional
    public void insertGlobalDefaultIfMissing() {
        if (preTranslationSettingRepository.existsByUserIsNull()) {
            return;
        }

        PreTranslationSetting setting = new PreTranslationSetting();
        setting.setUser(null);
        preTranslationSettingRepository.save(setting);
    }

    @Transactional(readOnly = true)
    public PreTranslationSetting getForUser(User user) {
        return preTranslationSettingRepository.findByUser(user)
                .orElseGet(this::getGlobalDefault);
    }

    private PreTranslationSetting getGlobalDefault() {
        return preTranslationSettingRepository.findFirstByUserIsNull()
                .orElseThrow(() ->
                        new IllegalStateException("Global Pre-translation default not initialized"));
    }

    @Transactional
    public PreTranslationSetting updateForUser(User user, PreTranslationSettingDTO dto) {
        if (user == null) {
            throw new IllegalArgumentException("Cannot update global default with this method");
        }

        PreTranslationSetting setting = preTranslationSettingRepository.findByUser(user)
            .orElseGet(() -> {
                PreTranslationSetting s = new PreTranslationSetting();
                s.setUser(user);
                return s;
            });

        setting.setOverwriteExistingTranslationsInTargetSegments(dto.overwriteExistingTranslationsInTargetSegments());
        setting.setPreTranslateOnJobCreation(dto.preTranslateOnJobCreation());
        setting.setPreTranslateFromTranslationMem(dto.preTranslateFromTranslationMem());
        setting.setTmThreshold(dto.tmThreshold());
        setting.setPreTranslateFromNonTranslatables(dto.preTranslateFromNonTranslatables());
        setting.setPreTranslateFromMachineTranslation(dto.preTranslateFromMachineTranslation());
        
        setting.setTransMemoryMatch_101Confirmed(dto.transMemoryMatch_101Confirmed());
        setting.setTransMemoryMatch_100Confirmed(dto.transMemoryMatch_100Confirmed());
        setting.setNonTranslatables_100Confirmed(dto.nonTranslatables_100Confirmed());
        setting.setMachineTranslationSuggestionsQpsConfirmed(dto.machineTranslationSuggestionsQpsConfirmed());
        setting.setQpsThreshold(dto.qpsThreshold());

        setting.setPreTranslated(dto.preTranslated());
        setting.setPreTranslatedAndAllSegmentsConfirmed(dto.preTranslatedAndAllSegmentsConfirmed());
        setting.setAllJobsPreTranslated(dto.allJobsPreTranslated());
        
        setting.setTransMemoryMatch_101Lock(dto.transMemoryMatch_101Lock());
        setting.setTransMemoryMatch_100Lock(dto.transMemoryMatch_100Lock());
        setting.setNonTranslatables_100Lock(dto.nonTranslatables_100Lock());
        setting.setMachineTranslationSuggestionsQpsLock(dto.machineTranslationSuggestionsQpsLock());

        return preTranslationSettingRepository.save(setting);
    }

    public PreTranslationSettingDTO toDTO(PreTranslationSetting s) {
        return new PreTranslationSettingDTO(
            s.isOverwriteExistingTranslationsInTargetSegments(),
            s.isPreTranslateOnJobCreation(),
            s.isPreTranslateOnJobCreation(),
            s.getTmThreshold(),
            s.isPreTranslateFromNonTranslatables(),
            s.isPreTranslateFromMachineTranslation(),
            s.isTransMemoryMatch_101Confirmed(),
            s.isTransMemoryMatch_100Confirmed(),
            s.isNonTranslatables_100Confirmed(),
            s.isMachineTranslationSuggestionsQpsConfirmed(),
            s.getQpsThreshold(),
            s.isPreTranslated(),
            s.isPreTranslatedAndAllSegmentsConfirmed(),
            s.isAllJobsPreTranslated(),
            s.isTransMemoryMatch_101Lock(),
            s.isTransMemoryMatch_100Lock(),
            s.isNonTranslatables_100Lock(),
            s.isMachineTranslationSuggestionsQpsLock()
        );
    }
    
}
