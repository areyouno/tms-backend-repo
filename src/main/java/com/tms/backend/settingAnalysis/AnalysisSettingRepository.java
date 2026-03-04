package com.tms.backend.settingAnalysis;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tms.backend.user.User;

@Repository
public interface AnalysisSettingRepository extends JpaRepository<AnalysisSetting, Long>{
    Optional<AnalysisSetting> findByUserAndAnalysisType(User user, AnalysisType type);

    Optional<AnalysisSetting> findByUserIsNullAndAnalysisType(AnalysisType type);
}
