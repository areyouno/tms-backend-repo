package com.tms.backend.setting;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.tms.backend.currency.CurrencyService;
import com.tms.backend.settingAnalysis.AnalysisSettingService;
import com.tms.backend.settingCatEditor.CatEditorSettingService;
import com.tms.backend.settingCompletedFilesNaming.CompletedFilesNamingSettingService;
import com.tms.backend.settingPreTranslation.PreTranslationSettingService;

@Component
public class DataInitializer implements ApplicationRunner {
    private final AnalysisSettingService analysisSettingService;
    private final CatEditorSettingService catEditorSettingService;
    private final CompletedFilesNamingSettingService completedFilesNamingSettingService;
    private final PreTranslationSettingService preTranslationSettingService;
    private final CurrencyService currencyService;

    public DataInitializer(
            AnalysisSettingService analysisSettingService,
            CatEditorSettingService catEditorSettingService,
            CompletedFilesNamingSettingService completedFilesNamingSettingService,
            PreTranslationSettingService preTranslationSettingService,
            CurrencyService currencyService
    ) {
        this.analysisSettingService = analysisSettingService;
        this.catEditorSettingService = catEditorSettingService;
        this.completedFilesNamingSettingService = completedFilesNamingSettingService;
        this.preTranslationSettingService = preTranslationSettingService;
        this.currencyService = currencyService;
    }

    @Override
    public void run(ApplicationArguments args) {
        analysisSettingService.insertGlobalDefaultIfMissing();
        catEditorSettingService.insertGlobalDefaultIfMissing();
        completedFilesNamingSettingService.insertGlobalDefaultIfMissing();
        preTranslationSettingService.insertGlobalDefaultIfMissing();
        currencyService.seedDefaultCurrencies();
    }
    
}
