package com.tms.backend.setting;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.project.ProjectAutomationRule;
import com.tms.backend.user.CustomUserDetails;
import com.tms.backend.user.User;
import com.tms.backend.user.UserService;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/settings/automation")
public class AutomationSettingController {

    private final AutomationSettingService service;
    private final UserService userService;

    public AutomationSettingController(AutomationSettingService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    @GetMapping("/user")
    public ResponseEntity<Set<String>> getOrCreateUserAutomationSetting(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String uid = userDetails.getUid(); // Extract uid from CustomUserDetails
        User user = userService.findByUid(uid)
        .orElseThrow(() -> new RuntimeException("User not found with uid: " + uid));

        AutomationSetting setting = service.getOrCreateUserAutomationSetting(user);

        Set<String> ruleNames = setting.getUserAutomationRules()
                .getEnabledRules()
                .stream()
                .map(ProjectAutomationRule::name)
                .collect(Collectors.toSet());
    
        return ResponseEntity.ok(ruleNames);
    }

    @PutMapping("/user")
    public ResponseEntity<Set<String>> updateUserAutomationSetting(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody Set<String> ruleNames) {
    
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String uid = userDetails.getUid(); // Extract uid from CustomUserDetails
        User user = userService.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("User not found with uid: " + uid));
        AutomationSetting setting = service.updateUserAutomationRules(user, ruleNames);

        Set<String> updatedRules = setting.getUserAutomationRules()
                .getEnabledRules()
                .stream()
                .map(ProjectAutomationRule::name)
                .collect(Collectors.toSet());

        return ResponseEntity.ok(updatedRules);
    }
}
