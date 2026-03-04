package com.tms.backend.settingCatEditor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.CatEditorSettingDTO;
import com.tms.backend.user.CustomUserDetails;
import com.tms.backend.user.User;
import com.tms.backend.user.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/settings/cat-editor")
@RequiredArgsConstructor
public class CatEditorSettingController {

    private final UserService userService;
    private final CatEditorSettingService catEditorSettingService;

    @GetMapping
    public CatEditorSettingDTO getSetting(@AuthenticationPrincipal CustomUserDetails userDetails) {
        // Get current user
        String uid = userDetails.getUid(); // Extract uid from CustomUserDetails
        User currentUser = userService.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("User not found with uid: " + uid));

        return catEditorSettingService.toDTO(catEditorSettingService.getForUser(currentUser));
    }

    @PutMapping("/update")
    public CatEditorSettingDTO update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CatEditorSettingDTO dto) {
        // Get current user
        String uid = userDetails.getUid(); // Extract uid from CustomUserDetails
        User currentUser = userService.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("User not found with uid: " + uid));
                
        return catEditorSettingService.toDTO(catEditorSettingService.updateForUser(currentUser, dto));
    }
    
}
