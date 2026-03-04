package com.tms.backend.settingCompletedFilesNaming;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.CompletedFilesNamingSettingDTO;
import com.tms.backend.user.CustomUserDetails;
import com.tms.backend.user.User;
import com.tms.backend.user.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/completed-files-naming")
@RequiredArgsConstructor
public class CompletedFilesNamingSettingController {

    private final CompletedFilesNamingSettingService completedFilesService;
    private final UserService userService;

    /**
     * get current user's setting.
     * Falls back to global default if user-specific setting does not exist.
     */
    @GetMapping
    public CompletedFilesNamingSettingDTO getSetting(@AuthenticationPrincipal CustomUserDetails userDetails) {
        // Get current user
        String uid = userDetails.getUid(); // Extract uid from CustomUserDetails
        User currentUser = userService.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("User not found with uid: " + uid));

        CompletedFilesNamingSetting setting = completedFilesService.getForUser(currentUser);
        return toDTO(setting);
    }

    /**
     * PUT updates the user's setting.
     * Creates a new record if none exists for the user.
     */
    @PutMapping("/update")
    public CompletedFilesNamingSettingDTO update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CompletedFilesNamingSettingDTO dto
    ) {
        
        // Get current user
        String uid = userDetails.getUid(); // Extract uid from CustomUserDetails
        User currentUser = userService.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("User not found with uid: " + uid));
        
        CompletedFilesNamingSetting updated = completedFilesService.updateForUser(
                currentUser,
                dto.folderName(),
                dto.hasNamingRule(),
                dto.namingRule()
        );
        return toDTO(updated);
    }

    // Helper to convert entity to DTO
    private CompletedFilesNamingSettingDTO toDTO(CompletedFilesNamingSetting s) {
        return new CompletedFilesNamingSettingDTO(
                s.getFolderName(),
                s.isHasNamingRule(),
                s.isHasNamingRule() ? s.getNamingRule() : null
        );
    }
}
