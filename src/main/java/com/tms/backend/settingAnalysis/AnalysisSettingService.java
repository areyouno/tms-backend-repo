package com.tms.backend.settingAnalysis;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tms.backend.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalysisSettingService {
    private final AnalysisSettingRepository analysisSettingRepo;

    // insert a default value (on first start up)
    @Transactional
    public void insertGlobalDefaultIfMissing() {
        boolean exists = analysisSettingRepo
                .findByUserIsNullAndAnalysisType(AnalysisType.DEFAULT)
                .isPresent();

        if (exists) return;

        AnalysisSetting defaultSetting = new AnalysisSetting();
        defaultSetting.setUser(null);
        defaultSetting.setAnalysisType(AnalysisType.DEFAULT);
        defaultSetting.setName("Analysis {projectName}");

        defaultSetting.setTransMemMatch(true);
        defaultSetting.setInternalFuzz(true);
        defaultSetting.setSeparateInternalFuzz(false);
        defaultSetting.setNonTranslatables(true);
        defaultSetting.setMachineTransSuggestion(true);

        defaultSetting.setConfirmedSegments(false);
        defaultSetting.setLockedSegments(false);
        defaultSetting.setExcludeNumbers(false);

        defaultSetting.setAnalyzeByProvider(false);
        defaultSetting.setAnalyzeByLanguage(false);

        defaultSetting.setScope(AnalysisScope.SOURCE);

        analysisSettingRepo.save(defaultSetting);
    }

    // get user setting; else, return default
    @Transactional(readOnly = true)
    public AnalysisSetting getUserSetting(User user) {
        return analysisSettingRepo
                .findByUserAndAnalysisType(user, AnalysisType.DEFAULT)
                .orElseGet(() -> analysisSettingRepo
                        .findByUserIsNullAndAnalysisType(AnalysisType.DEFAULT)
                        .orElseThrow(() -> new IllegalStateException("Global default missing")));
    }

    // update user setting
    @Transactional
    public AnalysisSetting saveUserSetting(User user, AnalysisSetting updated) {

        AnalysisSetting setting = analysisSettingRepo
                .findByUserAndAnalysisType(user, AnalysisType.DEFAULT)
                .orElseGet(() -> {
                    AnalysisSetting aSetting = analysisSettingRepo
                            .findByUserIsNullAndAnalysisType(AnalysisType.DEFAULT)
                            .orElseThrow();

                    AnalysisSetting clone = new AnalysisSetting();
                    BeanUtils.copyProperties(aSetting, clone, "id", "user");
                    clone.setUser(user);
                    return clone;
                });

        // apply changes
        setting.setTransMemMatch(updated.isTransMemMatch());
        setting.setInternalFuzz(updated.isInternalFuzz());
        setting.setSeparateInternalFuzz(updated.isSeparateInternalFuzz());
        setting.setNonTranslatables(updated.isNonTranslatables());
        setting.setMachineTransSuggestion(updated.isMachineTransSuggestion());

        setting.setConfirmedSegments(updated.isConfirmedSegments());
        setting.setLockedSegments(updated.isLockedSegments());
        setting.setExcludeNumbers(updated.isExcludeNumbers());

        setting.setAnalyzeByProvider(updated.isAnalyzeByProvider());
        setting.setAnalyzeByLanguage(updated.isAnalyzeByLanguage());
        setting.setScope(updated.getScope());

        return analysisSettingRepo.save(setting);
    }
}
