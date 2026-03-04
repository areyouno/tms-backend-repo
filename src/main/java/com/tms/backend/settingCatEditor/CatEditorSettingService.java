package com.tms.backend.settingCatEditor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tms.backend.dto.CatEditorSettingDTO;
import com.tms.backend.user.User;


@Service
public class CatEditorSettingService {
    private final CatEditorSettingRepository catEditorSettingRepository;

    public CatEditorSettingService(CatEditorSettingRepository catEditorSettingRepository){
        this.catEditorSettingRepository = catEditorSettingRepository;
    }
    
    @Transactional
    public void insertGlobalDefaultIfMissing() {
        if (catEditorSettingRepository.existsByUserIsNull()) {
            return;
        }

        CatEditorSetting setting = new CatEditorSetting();
        setting.setUser(null); // global default
        catEditorSettingRepository.save(setting);
    }

    @Transactional(readOnly = true)
    public CatEditorSetting getForUser(User user) {
        return catEditorSettingRepository.findByUser(user)
                .orElseGet(this::getGlobalDefault);
    }

    private CatEditorSetting getGlobalDefault() {
        return catEditorSettingRepository.findFirstByUserIsNull()
                .orElseThrow(() ->
                        new IllegalStateException("Global CAT editor default not initialized"));
    }

    @Transactional
    public CatEditorSetting updateForUser(User user, CatEditorSettingDTO dto) {

        if (user == null) {
        throw new IllegalArgumentException("Cannot update global default with this method");
        }

        CatEditorSetting setting = catEditorSettingRepository.findByUser(user)
                .orElseGet(() -> {
                    CatEditorSetting s = new CatEditorSetting();
                    s.setUser(user);
                    return s;
                });

        setting.setDisplayNonTranslatablesScoresInCatEditor(
                dto.displayNonTranslatablesScoresInCatEditor());
        setting.setSuggestMtOnlyForSegmentsWithTmMatchBelow(
                dto.suggestMtOnlyForSegmentsWithTmMatchBelow());
        setting.setDisplayPhraseQualityPerformanceScoreMatchesInCatEditor(
                dto.displayPhraseQualityPerformanceScoreMatchesInCatEditor());
        setting.setAutoPropagateRepetitions(dto.autoPropagateRepetitions());
        setting.setAutoPropagateToLockedRepetitions(dto.autoPropagateToLockedRepetitions());
        setting.setSetSegmentStatusConfirmedForRepetitions(
                dto.setSegmentStatusConfirmedForRepetitions());
        setting.setLockSubsequentRepetitions(dto.lockSubsequentRepetitions());

        return catEditorSettingRepository.save(setting);
    }

    public CatEditorSettingDTO toDTO(CatEditorSetting s) {
        return new CatEditorSettingDTO(
                s.isDisplayNonTranslatablesScoresInCatEditor(),
                s.isSuggestMtOnlyForSegmentsWithTmMatchBelow(),
                s.isDisplayPhraseQualityPerformanceScoreMatchesInCatEditor(),
                s.isAutoPropagateRepetitions(),
                s.isAutoPropagateToLockedRepetitions(),
                s.isSetSegmentStatusConfirmedForRepetitions(),
                s.isLockSubsequentRepetitions());
    }
}
