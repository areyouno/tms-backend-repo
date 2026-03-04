package com.tms.backend.settingPreTranslation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.PreTranslationSettingDTO;
import com.tms.backend.user.CustomUserDetails;
import com.tms.backend.user.User;
import com.tms.backend.user.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/settings/pre-translation")
@RequiredArgsConstructor
public class PreTranslationSettingController {
    private final UserService userService;
    private final PreTranslationSettingService preTranslationSettingService;

    @GetMapping
    public PreTranslationSettingDTO getSetting(@AuthenticationPrincipal CustomUserDetails userDetails) {
        // Get current user
        String uid = userDetails.getUid();
        User currentUser = userService.findByUid(uid)
            .orElseThrow(() -> new RuntimeException("User not found with uid: " + uid));

        return preTranslationSettingService.toDTO(preTranslationSettingService.getForUser(currentUser));
    }

    @PutMapping("/update")
    public PreTranslationSettingDTO update(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody PreTranslationSettingDTO dto) {
            String uid = userDetails.getUid();
            User currenUser = userService.findByUid(uid)
                    .orElseThrow(() -> new RuntimeException("User not found with uid: " + uid));

            return preTranslationSettingService.toDTO(preTranslationSettingService.updateForUser(currenUser, dto));
    }
}
