package com.tms.backend.settingAnalysis;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.AnalysisSettingUpdateRequest;
import com.tms.backend.user.CustomUserDetails;
import com.tms.backend.user.User;
import com.tms.backend.user.UserService;


@RestController
@RequestMapping("/api/settings/analysis")
public class AnalysisSettingController {
    private final AnalysisSettingService analysisService;
    private final UserService userService;

    public AnalysisSettingController(
        AnalysisSettingService analysisService,
        UserService userService){
        this.analysisService = analysisService;
        this.userService = userService;
    }

    @GetMapping
    public AnalysisSetting fetchDefaultSetting(@AuthenticationPrincipal CustomUserDetails userDetails) {

        // Get current user
        String uid = userDetails.getUid(); // Extract uid from CustomUserDetails
        User currentUser = userService.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("User not found with uid: " + uid));

        return analysisService.getUserSetting(currentUser);
    }

    @PutMapping("/update")
    public ResponseEntity<Void> updateAnalysisSetting(
            @RequestBody AnalysisSettingUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Get current user
        String uid = userDetails.getUid(); // Extract uid from CustomUserDetails
        User currentUser = userService.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("User not found with uid: " + uid));

        // Map DTO request to entity
        AnalysisSetting aSetting = new AnalysisSetting();
        aSetting.setTransMemMatch(request.transMemMatch());
        aSetting.setInternalFuzz(request.internalFuzz());
        aSetting.setSeparateInternalFuzz(request.separateInternalFuzz());
        aSetting.setNonTranslatables(request.nonTranslatables());
        aSetting.setMachineTransSuggestion(request.machineTransSuggestion());

        aSetting.setConfirmedSegments(request.confirmedSegments());
        aSetting.setLockedSegments(request.lockedSegments());
        aSetting.setExcludeNumbers(request.numbers());

        aSetting.setAnalyzeByProvider(request.provider());
        aSetting.setAnalyzeByLanguage(request.language());
        aSetting.setScope(request.scope());

        // Save (copy-on-write handled in service)
        analysisService.saveUserSetting(currentUser, aSetting);

        return ResponseEntity.noContent().build();
    }

}
